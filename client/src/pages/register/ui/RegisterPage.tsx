// @pages/register/index.tsx
import api from "@lib/api";
import { AnimatePresence, motion } from "framer-motion";
import { useState } from "react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { useNavigate } from "react-router-dom";

type RegisterData = {
  name: string;
  email: string;
  password: string;
};

const INITIAL_DATA: RegisterData = { name: "", email: "", password: "" };

const STEPS = [
  { id: "name", title: "Как к вам обращаться?", progress: 20 },
  { id: "email", title: "Укажите почту", progress: 40 },
  { id: "password", title: "Придумайте пароль", progress: 60 },
  { id: "confirm", title: "Проверьте данные", progress: 80 },
] as const;

const slide = {
  initial: { opacity: 0, x: 40 },
  animate: { opacity: 1, x: 0 },
  exit: { opacity: 0, x: -40 },
  transition: { duration: 0.25 },
};

export const RegisterPage = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [stepIndex, setStepIndex] = useState(0);
  const [data, setData] = useState<RegisterData>(INITIAL_DATA);
  const [error, setError] = useState("");
  const [done, setDone] = useState(false);
  const navigate = useNavigate();

  const step = STEPS[stepIndex];
  const progress = done ? 100 : step.progress;

  const setField =
    (field: keyof RegisterData) =>
    (e: React.ChangeEvent<HTMLInputElement>) =>
      setData((prev) => ({ ...prev, [field]: e.target.value }));

  const validate = (): string => {
    switch (step.id) {
      case "name":
        return data.name.trim().length >= 2 && data.name.trim().length <= 100 ? "" : "Введите имя (от 2 до 100 символов)";
      case "email":
        return /^\S+@\S+\.\S+$/.test(data.email) ? "" : "Почта записана неверно";
      case "password":
        return data.password.length >= 12 ? "" : "Пароль должен быть не короче 12 символов";
      default:
        return "";
    }
  };

  const next = async () => {
    const err = validate();
    if (err) return setError(err);
    setError("");

    if (stepIndex < STEPS.length - 1) {
      setStepIndex((i) => i + 1);
      return;
    }

    try {
      await api.post("/auth/register", data);
      setDone(true);
    } catch {
      setError("Не удалось создать аккаунт. Возможно почта занята или сервер временно недоступен");
    }
  };

  const back = () => {
    setError("");
    setStepIndex((i) => Math.max(0, i - 1));
  };

  return (
    <div className="mx-auto flex w-full max-w-md flex-col gap-4">
      <h1 className="font-oswald text-4xl">Регистрация</h1>

      <div className="flex items-center gap-4">
        <div className="w-full bg-secondary rounded-full h-2 overflow-hidden">
          <motion.div
            className="bg-accent h-full rounded-full"
            style={{ transformOrigin: "0 50%" }}
            initial={{ scaleX: 0 }}
            animate={{ scaleX: progress / 100 }}
            transition={{ type: "spring", stiffness: 120, damping: 20 }}
          />
        </div>
        <span className="w-12 text-right tabular-nums">{progress}%</span>
      </div>

      <AnimatePresence mode="wait">
        {done ? (
          <motion.div key="done" {...slide} className="flex flex-col gap-4">
            <h2 className="text-2xl">Аккаунт создан</h2>
            <p className="text-primary">Теперь можно войти с вашей почтой и паролем</p>
            <button className="btn-accent" onClick={() => navigate("/login")}>
              Войти
            </button>
          </motion.div>
        ) : (
          <motion.div
            key={step.id}
            {...slide}
            className="flex flex-col gap-4"
          >
            <h2 className="text-xl">{step.title}</h2>

            <form
              className="flex flex-col gap-4"
              onSubmit={(e) => { e.preventDefault(); next(); }}
            >
              {step.id === "name" && (
                <input
                  autoFocus
                  className="bg-secondary p-2 rounded-xl"
                  placeholder="Иванов Иван"
                  value={data.name}
                  onChange={setField("name")}
                />
              )}

              {step.id === "email" && (
                <input
                  autoFocus
                  type="email"
                  className="bg-secondary p-2 rounded-xl"
                  placeholder="IvanovIvan123@email.com"
                  value={data.email}
                  onChange={setField("email")}
                />
              )}

              {step.id === "password" && (
                <div className='flex gap-2'>
                  <input
                  autoFocus
                  required
                  type={showPassword ? "text" : "password"}
                  placeholder="Пароль_123"
                  name='userPassword'
                  value={data.password}
                  onChange={setField("password")}
                  className='p-2 bg-secondary rounded-lg'
                  />
                  <button 
                  type='button' 
                  onClick={() => setShowPassword(prev => !prev)} 
                  className='min-h-full px-2 rounded-lg bg-secondary hover:bg-primary active:scale-90 transition-all'
                  >
                    {showPassword ? <FaEyeSlash /> : <FaEye />}
                  </button>
                </div>
              )}

              {step.id === "confirm" && (
                <ul className="flex flex-col gap-2 rounded-xl border border-primary p-4">
                  <li className="flex justify-between">
                    <span className="text-primary">Имя:</span> {data.name}
                  </li>
                  <li className="flex justify-between">
                    <span className="text-primary">Почта:</span> {data.email}
                  </li>
                  <li className="flex justify-between">
                    <span className="text-primary">Пароль:</span>
                    <div className="flex gap-2">
                      <span>{"•".repeat(data.password.length)}</span>
                      <span>{`(${data.password.length} символов)`}</span>
                    </div>
                  </li>
                </ul>
              )}
              {error && <motion.p layout className="text-sm text-err bg-err-bg p-1 px-2 rounded-xl w-fit">{error}</motion.p>}

              <motion.div layout className="flex gap-2">
                {stepIndex > 0 && (
                  <button type="button" className="bg-secondary p-1 px-2 rounded-lg" onClick={back}>
                    Назад
                  </button>
                )}
                <button type="submit" className="bg-accent p-1 px-2 rounded-lg">
                  {stepIndex === STEPS.length - 1 ? "Подтвердить" : "Далее"}
                </button>
              </motion.div>
            </form>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};