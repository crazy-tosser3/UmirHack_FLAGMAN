// src/pages/LoginPage.jsx
import { useAuth } from '@shared/auth/auth-service';
import { useState } from 'react';
import { FaExclamationTriangle, FaEye, FaEyeSlash } from 'react-icons/fa';
import { NavLink, useSearchParams } from 'react-router-dom';
import LoginBtn from './LoginBtn';
import { PageTitle } from '@shared/page-title';
import { useRedirectTo } from '@shared/hooks/redirect-to';
import type { AxiosError } from 'axios';
import { TelegramAuth } from '@widgets/telegram-auth';
import type { TelegramAuthData } from '@lib/types';
import { apiErrorMessage } from '@lib/utils';

export function LoginPage() {
  const [err, setErr] = useState<AxiosError | null>(null);
  const [searchParams] = useSearchParams();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [tgError, setTgError] = useState<string | null>(null);

  const { login, loginTelegram } = useAuth();
  
  const redirectPath = searchParams.get('redirect') ?? '/';
  const redirectTo = useRedirectTo(redirectPath);

  const handleSubmit = async (event: React.SubmitEvent) => {
    event.preventDefault();

    try {
      await login(email, password);
      redirectTo();
    } catch (err) {
      setErr(err as AxiosError)
    }
  };

  const handleTelegram = async (data: TelegramAuthData) => {
    setTgError(null);
    try {
      await loginTelegram(data);
      redirectTo();
    } catch (err) {
      setTgError(apiErrorMessage(err, "Не удалось войти через Telegram"));
    }
  };

  return (
    <div className='flex bg-bg flex-col sm:flex-row shadow-lg gap-4 mx-auto rounded-xl border border-accent overflow-hidden'>
      <PageTitle title='Вход' />
      <div className='bg-accent flex flex-col text-center sm:text-left justify-center gap-4 p-4'>
        <h1 className='text-5xl font-oswald'>Добро пожаловать!</h1>
        <p className='text-sm'>Чтобы продолжить работу войдите в аккаунт</p>
      </div>
      <div className='flex flex-col gap-4 p-4'>
        <form onSubmit={handleSubmit} className='w-full flex flex-col gap-3 p-2 border border-secondary shadow-md rounded-md m-auto'>
          <label htmlFor="userEmail">Почта</label>
          <input
            required
            type="email"
            value={email}
            onChange={event => setEmail(event.target.value.trim())}
            placeholder="IvanovIvan123@email.com"
            name='userEmail'
            className='p-2 bg-secondary rounded-lg'
            />
          <label htmlFor="userPassword">Пароль</label>
          <div className='flex gap-2'>
            <input
            required
            type={showPassword ? "text" : "password"}
            value={password}
            onChange={event => setPassword(event.target.value.trim())}
            placeholder="Пароль_123"
            name='userPassword'
            className='p-2 bg-secondary rounded-lg w-full'
            />
            <button 
            type='button' 
            onClick={() => setShowPassword(prev => !prev)} 
            className='min-h-full px-2 rounded-lg bg-secondary hover:bg-primary active:scale-90 transition-all'
            >
              {showPassword ? <FaEyeSlash /> : <FaEye />}
            </button>
          </div>
          <NavLink to="/password-reset" className="text-sm">Не помню пароль</NavLink>
          <LoginBtn />
          {!!err && <div className='bg-err-bg text-err flex items-center justify-between gap-2 rounded-lg p-2'>
            <FaExclamationTriangle />
            <span>{err.status === 401 ? "Неверный логин или пароль" : "Произошла неизвестная ошибка, попробуйте войти позже"}</span>
          </div>}
        </form>
        <div className='flex flex-col gap-2 p-2 border border-secondary shadow-md rounded-md'>
          <div className='flex items-center gap-2 text-sm text-primary'>
            <span className='flex-1 border-t border-secondary' />
            <span>или для менеджеров и администраторов</span>
            <span className='flex-1 border-t border-secondary' />
          </div>
          <TelegramAuth onAuth={handleTelegram} className='justify-center' />
          {tgError && <div className='bg-err-bg text-err flex items-center justify-between gap-2 rounded-lg p-2 text-sm'>
            <FaExclamationTriangle className='shrink-0' />
            <span>{tgError}</span>
          </div>}
        </div>
        <div className='text-primary flex gap-2 m-auto items-center justify-center'>
          <span className='text-center'>
            Нет аккаунта?
          </span>
          <NavLink to="/register" className='p-1 px-2 rounded-md bg-secondary'>
            Зарегистрироваться
          </NavLink>
        </div>

      </div>
    </div>
  );
}