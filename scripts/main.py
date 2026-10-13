#!/usr/bin/env python3

import argparse
import asyncio
import os
import random
import signal
import sys
import uuid
from collections import Counter
from datetime import UTC, datetime, timedelta

import httpx

UTC = UTC

HOURLY_LOAD = [
    0.05,
    0.02,
    0.0,
    0.0,
    0.0,
    0.0,
    0.05,
    0.2,
    0.35,
    0.4,
    0.5,
    0.8,
    1.0,
    1.0,
    0.7,
    0.45,
    0.4,
    0.55,
    0.85,
    1.0,
    0.95,
    0.7,
    0.4,
    0.15,
]

STOP_REASONS = [
    "сломалась печь",
    "нет повара на станции",
    "проблема с качеством",
    "закончилась упаковка",
]


def iso(dt):
    return dt.astimezone(UTC).strftime("%Y-%m-%dT%H:%M:%SZ")


def error_code(response):
    try:
        return response.json()["error"]["code"]
    except (ValueError, KeyError, TypeError):
        return str(response.status_code)


def error_details(response):
    try:
        return response.json()["error"].get("details") or []
    except (ValueError, KeyError, TypeError, AttributeError):
        return []


class Clock:
    def __init__(self, start_hour, speed):
        loop = asyncio.get_running_loop()
        self.speed = speed
        self.real_start = loop.time()
        today = datetime.now().replace(minute=0, second=0, microsecond=0)
        self.sim_start = today.replace(hour=start_hour)

    def now(self):
        elapsed = asyncio.get_running_loop().time() - self.real_start
        return self.sim_start + timedelta(seconds=elapsed * self.speed)

    def load(self):
        t = self.now()
        h = t.hour + t.minute / 60
        a, b = HOURLY_LOAD[int(h) % 24], HOURLY_LOAD[(int(h) + 1) % 24]
        return a + (b - a) * (h - int(h))

    async def sleep_sim(self, sim_seconds):
        await asyncio.sleep(max(0.05, sim_seconds / self.speed))


class Api:
    def __init__(self, base_url, email, password):
        self.client = httpx.AsyncClient(
            base_url=base_url.rstrip("/") + "/api/v1", timeout=30
        )
        self.email = email
        self.password = password
        self.token = None
        self._login_lock = asyncio.Lock()

    async def login(self):
        async with self._login_lock:
            r = await self.client.post(
                "/auth/login", json={"email": self.email, "password": self.password}
            )
            if r.status_code != 200:
                sys.exit("Не удалось войти как %s: %s" % (self.email, error_code(r)))
            data = r.json()
            if data["user"]["role"] not in ("manager", "admin"):
                sys.exit(
                    "Нужна роль manager или admin, а у %s — %s"
                    % (self.email, data["user"]["role"])
                )
            self.token = data["accessToken"]

    async def request(self, method, path, **kwargs):
        for attempt in range(2):
            headers = {
                **kwargs.pop("headers", {}),
                "Authorization": "Bearer " + self.token,
            }
            try:
                r = await self.client.request(method, path, headers=headers, **kwargs)
            except httpx.TransportError as e:
                raise SystemExit(
                    "Бэкенд недоступен (%s): %s" % (self.client.base_url, e)
                )
            if r.status_code == 401 and attempt == 0:
                await self.login()
                kwargs["headers"] = {
                    k: v for k, v in headers.items() if k != "Authorization"
                }
                continue
            return r
        return r

    async def get_all(self, path):
        items, page = [], 1
        while True:
            sep = "&" if "?" in path else "?"
            r = await self.request("GET", "%s%spage=%d&limit=100" % (path, sep, page))
            r.raise_for_status()
            data = r.json()
            items += data["items"]
            if len(items) >= data["total"] or not data["items"]:
                return items
            page += 1

    async def close(self):
        await self.client.aclose()


class Restaurant:
    def __init__(self, api, clock, args):
        self.api = api
        self.clock = clock
        self.args = args
        self.dishes = []
        self.ingredients = {}
        self.popularity = {}
        self.stopped = set()
        self.hot = set()
        self.manual_stops = {}
        self.stats = Counter()
        self.shortages = Counter()

    async def load_catalog(self):
        ingredients = await self.api.get_all("/ingredients")
        self.ingredients = {i["id"]: i for i in ingredients}
        dishes = await self.api.get_all("/dishes")
        self.dishes = [d for d in dishes if d.get("isActive") and d["ingredients"]]
        if not self.dishes:
            sys.exit(
                "Нет активных блюд с рецептами — сначала заполните каталог (scripts/demo-recipes.sql)"
            )
        self.popularity = {d["id"]: random.lognormvariate(0, 0.6) for d in self.dishes}

    async def refresh_menu_state(self):
        r = await self.api.request("GET", "/stop-list?limit=100")
        if r.status_code == 200:
            self.stopped = {i["dishId"] for i in r.json()["items"]}
        r = await self.api.request("GET", "/offers?limit=10")
        if r.status_code == 200:
            self.hot = {i["dishId"] for i in r.json()["items"]}

    def pick_dishes(self):
        available = [d for d in self.dishes if d["id"] not in self.stopped]
        if not available:
            return []
        weights = [
            self.popularity[d["id"]] * (2.5 if d["id"] in self.hot else 1)
            for d in available
        ]
        count = min(
            len(available), random.choices([1, 2, 3, 4], weights=[45, 30, 17, 8])[0]
        )
        chosen = []
        while len(chosen) < count:
            dish = random.choices(available, weights=weights)[0]
            if dish not in chosen:
                chosen.append(dish)
        return chosen

    def build_order(self):
        items = []
        for dish in self.pick_dishes():
            item = {
                "dishId": dish["id"],
                "quantity": random.choices([1, 2, 3], weights=[75, 20, 5])[0],
            }
            if (
                len(dish["ingredients"]) > 1
                and random.random() < self.args.exclude_chance
            ):
                item["excludedIngredientIds"] = [
                    random.choice(dish["ingredients"])["ingredientId"]
                ]
            items.append(item)
        return {"externalOrderId": "SIM-" + uuid.uuid4().hex[:10], "items": items}

    async def place_order(self, order, key):
        return await self.api.request(
            "POST", "/orders", json=order, headers={"Idempotency-Key": key}
        )

    async def handle_shortage(self, order, response):
        """Не хватило ингредиентов: кто-то согласится без них, кто-то откажется от блюда."""
        missing = {d["ingredientId"] for d in error_details(response)}
        self.shortages.update(missing)
        recipes = {
            d["id"]: {i["ingredientId"] for i in d["ingredients"]} for d in self.dishes
        }

        items = []
        for item in order["items"]:
            lacking = recipes[item["dishId"]] & missing
            if not lacking:
                items.append(item)
            elif (
                len(lacking) < len(recipes[item["dishId"]])
                and random.random() < self.args.accept_without_chance
            ):
                excluded = set(item.get("excludedIngredientIds", [])) | lacking
                items.append({**item, "excludedIngredientIds": sorted(excluded)})
                self.stats["без ингредиента"] += 1
        if not items:
            return None
        return {
            **order,
            "externalOrderId": order["externalOrderId"] + "-R",
            "items": items,
        }

    async def waiter(self, number):
        while True:
            load = self.clock.load()
            if load <= 0.01:
                await self.clock.sleep_sim(15 * 60)
                continue
            rate_per_hour = self.args.orders_per_hour * load / self.args.waiters
            await self.clock.sleep_sim(random.expovariate(rate_per_hour / 3600))

            order = self.build_order()
            if not order["items"]:
                self.stats["всё в стопе"] += 1
                continue

            for attempt in range(2):
                key = str(uuid.uuid4())
                r = await self.place_order(order, key)
                if r.status_code == 201:
                    self.stats["заказов"] += 1
                    if any(i.get("excludedIngredientIds") for i in order["items"]):
                        self.stats["с исключениями"] += 1
                    if random.random() < self.args.retry_chance:
                        again = await self.place_order(order, key)
                        same = (
                            again.status_code == 201
                            and again.json()["id"] == r.json()["id"]
                        )
                        self.stats["ретраев ок" if same else "ретраев FAIL"] += 1
                    break
                code = error_code(r)
                if code == "INSUFFICIENT_STOCK" and attempt == 0:
                    self.stats["нехватка"] += 1
                    order = await self.handle_shortage(order, r)
                    if order is None:
                        self.stats["гость ушёл"] += 1
                        break
                    continue
                if code == "DISH_UNAVAILABLE":
                    self.stats["блюдо в стопе"] += 1
                    await self.refresh_menu_state()
                else:
                    self.stats["ошибка " + code] += 1
                break

    async def deliver(self, quantities, label):
        now = datetime.now(UTC)
        items = []
        for ingredient_id, quantity in quantities.items():
            unit = self.ingredients.get(ingredient_id, {}).get("unit", "g")
            quantity = (
                max(1, round(quantity))
                if unit == "pcs"
                else round(max(quantity, 0.1), 1)
            )
            short = random.random() < self.args.short_expiry_chance
            hours = random.uniform(4, 30) if short else random.uniform(72, 240)
            items.append(
                {
                    "ingredientId": ingredient_id,
                    "quantity": quantity,
                    "expiresAt": iso(now + timedelta(hours=hours)),
                }
            )
        if not items:
            return
        number = "SIM-%s-%s" % (label, uuid.uuid4().hex[:6].upper())
        for start in range(0, len(items), 500):
            body = {
                "deliveryNumber": number,
                "receivedAt": iso(now),
                "items": items[start : start + 500],
            }
            r = await self.api.request(
                "POST",
                "/deliveries",
                json=body,
                headers={"Idempotency-Key": number + str(start)},
            )
            if r.status_code == 201:
                self.stats["поставок"] += 1
                self.log("Поставка %s: %d позиций" % (number, len(body["items"])))
            else:
                self.stats["ошибка поставки " + error_code(r)] += 1

    async def initial_stock(self):
        r = await self.api.request("GET", "/stock?limit=100")
        if r.status_code != 200:
            return
        stocked = {
            i["ingredientId"]
            for i in r.json()["items"]
            if float(i["availableQuantity"]) > 0
        }
        per_portion = Counter()
        for d in self.dishes:
            for i in d["ingredients"]:
                per_portion[i["ingredientId"]] = max(
                    per_portion[i["ingredientId"]], float(i["quantity"])
                )
        missing = {
            k: v * self.args.orders_per_hour * 3
            for k, v in per_portion.items()
            if k not in stocked
        }
        if missing:
            await self.deliver(missing, "START")

    async def supplier(self):
        while True:
            r = await self.api.request(
                "GET", "/analytics/supply-suggestions?coverDays=2"
            )
            if r.status_code != 200:
                await self.clock.sleep_sim(self.args.delivery_every_hours * 3600)
                continue
            plan = {
                i["ingredientId"]: float(i["suggestedQuantity"])
                * random.uniform(0.8, 1.2)
                for i in r.json()["items"]
                if float(i["suggestedQuantity"]) > 0
            }
            for ingredient_id in list(plan):
                if random.random() < 0.1:
                    plan.pop(ingredient_id)
            await self.deliver(plan, "PLAN")
            await self.clock.sleep_sim(self.args.delivery_every_hours * 3600)

    async def emergency_supplier(self):
        while True:
            await self.clock.sleep_sim(45 * 60)
            top = [i for i, n in self.shortages.most_common(5) if n >= 3]
            self.shortages.clear()
            if not top:
                continue
            per_portion = {
                i["ingredientId"]: float(i["quantity"])
                for d in self.dishes
                for i in d["ingredients"]
            }
            await self.deliver({i: per_portion.get(i, 100) * 40 for i in top}, "URGENT")

    async def manager(self):
        while True:
            await self.clock.sleep_sim(random.uniform(1, 3) * 3600)
            if self.manual_stops and random.random() < 0.6:
                dish_id = random.choice(list(self.manual_stops))
                r = await self.api.request("DELETE", "/stop-list/" + dish_id)
                if r.status_code == 200:
                    self.manual_stops.pop(dish_id)
                    self.stats["снято со стопа"] += 1
                    self.log("Менеджер снял со стопа: %s" % r.json()["dishName"])
            elif len(self.manual_stops) < 2:
                dish = random.choice(self.dishes)
                reason = random.choice(STOP_REASONS)
                r = await self.api.request("PUT", "/stop-list/" + dish["id"])
                if r.status_code == 200:
                    self.manual_stops[dish["id"]] = reason
                    self.stats["ручных стопов"] += 1
                    self.log(
                        "Менеджер поставил на стоп: %s (%s)" % (dish["name"], reason)
                    )
            await self.refresh_menu_state()

    async def release_manual_stops(self):
        for dish_id in list(self.manual_stops):
            await self.api.request("DELETE", "/stop-list/" + dish_id)
        self.manual_stops.clear()

    async def menu_watcher(self):
        while True:
            await self.refresh_menu_state()
            await asyncio.sleep(5)

    async def reporter(self):
        while True:
            await asyncio.sleep(self.args.report_every)
            self.log(
                "загрузка %3d%% | стоп-лист %d | горячие %d | %s"
                % (
                    round(self.clock.load() * 100),
                    len(self.stopped),
                    len(self.hot),
                    ", ".join("%s %d" % kv for kv in sorted(self.stats.items()))
                    or "пока тихо",
                )
            )

    def log(self, message):
        print("[%s] %s" % (self.clock.now().strftime("%H:%M"), message), flush=True)


async def main(args):
    if args.seed is not None:
        random.seed(args.seed)

    api = Api(args.base_url, args.email, args.password)
    await api.login()
    clock = Clock(args.start_hour, args.speed)
    restaurant = Restaurant(api, clock, args)
    await restaurant.load_catalog()
    restaurant.log(
        "Каталог: %d блюд, %d ингредиентов. Скорость x%g, Ctrl+C — остановить"
        % (len(restaurant.dishes), len(restaurant.ingredients), args.speed)
    )

    await restaurant.initial_stock()
    await restaurant.refresh_menu_state()

    tasks = [restaurant.waiter(n) for n in range(args.waiters)]
    tasks += [restaurant.menu_watcher(), restaurant.reporter()]
    if not args.no_supplier:
        tasks += [restaurant.supplier(), restaurant.emergency_supplier()]
    if not args.no_manager:
        tasks.append(restaurant.manager())

    running = [asyncio.create_task(t) for t in tasks]
    stop = asyncio.Event()
    loop = asyncio.get_running_loop()
    for sig in (signal.SIGINT, signal.SIGTERM):
        try:
            loop.add_signal_handler(sig, stop.set)
        except NotImplementedError:  # Windows
            signal.signal(sig, lambda *_: loop.call_soon_threadsafe(stop.set))

    waiters = [asyncio.create_task(stop.wait())]
    if args.duration:
        waiters.append(asyncio.create_task(asyncio.sleep(args.duration)))
    done, _ = await asyncio.wait(running + waiters, return_when=asyncio.FIRST_COMPLETED)
    for task in done:
        if task in running and task.exception():
            print("Сценарий упал:", repr(task.exception()), file=sys.stderr)

    for task in running + waiters:
        task.cancel()
    await asyncio.gather(*running, *waiters, return_exceptions=True)

    await restaurant.release_manual_stops()
    restaurant.log(
        "Итого: "
        + (
            ", ".join("%s %d" % kv for kv in sorted(restaurant.stats.items()))
            or "ничего"
        )
    )
    await api.close()


def parse_args():
    p = argparse.ArgumentParser(description="Имитация работы ресторана через API")
    p.add_argument(
        "--base-url", default=os.environ.get("FLAGMAN_URL", "http://127.0.0.1:8080")
    )
    p.add_argument(
        "--email", default=os.environ.get("FLAGMAN_EMAIL", "admin@yandex.ru")
    )
    p.add_argument(
        "--password", default=os.environ.get("FLAGMAN_PASSWORD", "adminadmin1234")
    )
    p.add_argument(
        "--speed",
        type=float,
        default=60,
        help="во сколько раз время симуляции быстрее реального (60: минута = час)",
    )
    p.add_argument(
        "--start-hour",
        type=int,
        default=datetime.now().hour,
        help="с какого часа суток начать",
    )
    p.add_argument(
        "--duration",
        type=float,
        help="сколько реальных секунд работать (по умолчанию — до Ctrl+C)",
    )
    p.add_argument(
        "--waiters",
        type=int,
        default=4,
        help="сколько официантов параллельно отправляют заказы",
    )
    p.add_argument(
        "--orders-per-hour",
        type=float,
        default=40,
        help="заказов в час в пиковую загрузку",
    )
    p.add_argument(
        "--exclude-chance",
        type=float,
        default=0.1,
        help="вероятность, что гость попросит убрать ингредиент",
    )
    p.add_argument(
        "--accept-without-chance",
        type=float,
        default=0.6,
        help="вероятность согласиться на блюдо без недостающего ингредиента",
    )
    p.add_argument(
        "--retry-chance",
        type=float,
        default=0.03,
        help="вероятность повторной отправки заказа с тем же Idempotency-Key",
    )
    p.add_argument(
        "--delivery-every-hours",
        type=float,
        default=4,
        help="интервал плановых поставок (часы симуляции)",
    )
    p.add_argument(
        "--short-expiry-chance",
        type=float,
        default=0.25,
        help="доля партий с коротким сроком годности",
    )
    p.add_argument(
        "--report-every",
        type=float,
        default=10,
        help="как часто печатать сводку, реальные секунды",
    )
    p.add_argument("--no-supplier", action="store_true", help="без поставок")
    p.add_argument("--no-manager", action="store_true", help="без ручного стоп-листа")
    p.add_argument("--seed", type=int, help="seed для воспроизводимости")
    args = p.parse_args()
    if not 0 <= args.start_hour <= 23:
        p.error("--start-hour — от 0 до 23")
    if args.speed <= 0 or args.waiters < 1 or args.orders_per_hour <= 0:
        p.error("--speed, --waiters и --orders-per-hour должны быть положительными")
    return args


if __name__ == "__main__":
    try:
        asyncio.run(main(parse_args()))
    except KeyboardInterrupt:
        pass
