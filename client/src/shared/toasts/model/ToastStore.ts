import { create } from "zustand";

interface Toast {
  id: string; 
  msg: string;
  status: "error" | "info" | "success";
}

interface ToastStore {
  toasts: Toast[];

  newToast: (msg: string, status?: Toast["status"]) => void;

  removeToast: (id: string) => void; 
}

export const useToastStore = create<ToastStore>((set, get) => ({
  toasts: [],

  newToast: (msg, status = "info") => {
    const id = Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
    const toast: Toast = {
      id,
      msg,
      status,
    };

    set((state) => ({
      toasts: [...state.toasts, toast],
    }));

    const timeout = status === 'success' ? 2500 : 5000;
    setTimeout(() => {
      get().removeToast(id);
    }, timeout);
  },

  removeToast: (id) => {
    set((state) => ({
      toasts: state.toasts.filter((toast) => toast.id !== id),
    }));
  },
}));