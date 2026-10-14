import { AnimatePresence, motion } from "framer-motion";
import { useEffect, useRef, useState } from "react";
import { FaArrowUp } from "react-icons/fa6";

const SHOW_AFTER = 300;
const MIN_SCROLLABLE = 200;

type Scroller = Element | Window;

const scrollTopOf = (target: Scroller) =>
  target instanceof Window ? window.scrollY : target.scrollTop;

const resolveTarget = (eventTarget: EventTarget | null): Scroller | null => {
  if (eventTarget === document || eventTarget === window || eventTarget === document.documentElement || eventTarget === document.body) {
    return window;
  }
  if (!(eventTarget instanceof HTMLElement)) return null;
  if (eventTarget.scrollHeight - eventTarget.clientHeight < MIN_SCROLLABLE) return null;
  if (eventTarget.closest("[role='dialog'], select, [data-scroll-top-ignore]")) return null;
  return eventTarget;
};

export const ScrollToTop = () => {
  const [visible, setVisible] = useState(false);
  const targetRef = useRef<Scroller | null>(null);

  useEffect(() => {
    let frame = 0;

    const onScroll = (event: Event) => {
      const target = resolveTarget(event.target);
      if (!target) return;
      cancelAnimationFrame(frame);
      frame = requestAnimationFrame(() => {
        const top = scrollTopOf(target);
        if (top > SHOW_AFTER) {
          targetRef.current = target;
          setVisible(true);
        } else if (targetRef.current === target || targetRef.current === null) {
          targetRef.current = null;
          setVisible(false);
        }
      });
    };

    document.addEventListener("scroll", onScroll, { capture: true, passive: true });
    return () => {
      cancelAnimationFrame(frame);
      document.removeEventListener("scroll", onScroll, { capture: true });
    };
  }, []);

  const scrollUp = () => {
    const target = targetRef.current ?? window;
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    target.scrollTo({ top: 0, behavior: reduceMotion ? "auto" : "smooth" });
  };

  return (
    <AnimatePresence>
      {visible && (
        <motion.button
          type="button"
          aria-label="Наверх"
          onClick={scrollUp}
          initial={{ opacity: 0, y: 16, scale: 0.9 }}
          animate={{ opacity: 1, y: 0, scale: 1 }}
          exit={{ opacity: 0, y: 16, scale: 0.9 }}
          transition={{ type: "spring", stiffness: 400, damping: 30 }}
          style={{ bottom: "calc(var(--bottom-nav-h, 0px) + max(1rem, env(safe-area-inset-bottom) + 1rem))" }}
          className="fixed left-1/2 -translate-x-1/2 z-40 flex items-center gap-2 rounded-full bg-text text-bg px-4 py-2 text-sm shadow-lg active:scale-95 transition-transform"
        >
          <FaArrowUp size={12} />
          Наверх
        </motion.button>
      )}
    </AnimatePresence>
  );
};
