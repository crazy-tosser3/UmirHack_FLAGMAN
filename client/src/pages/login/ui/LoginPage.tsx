// src/pages/LoginPage.jsx
import { useAuth } from '@shared/auth/auth-service';
import { useState } from 'react';
import { FaEye, FaEyeSlash } from 'react-icons/fa';
import { NavLink, useNavigate } from 'react-router-dom';
import LoginBtn from './LoginBtn';
import { PageTitle } from '@shared/page-title';

export function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const { login } = useAuth();
  
  const navigate = useNavigate();

  const handleSubmit = async (event: React.SubmitEvent) => {
    event.preventDefault();

    try {
      await login(email, password);
      navigate('/dashboard', { replace: true });
    } catch {
      alert('Ошибка авторизации');
    }
  };

  return (
    <div className='flex flex-col gap-4'>
      <PageTitle title='Вход' />

      <h1 className='text-5xl font-oswald text-center'>Добро пожаловать</h1>
      <p className='text-center text-sm'>Чтобы продолжить работу войдите в аккаунт</p>
      <form onSubmit={handleSubmit} className='flex flex-col gap-3 w-fit p-2 border border-secondary shadow-md rounded-xl m-auto'>
        <label htmlFor="userEmail">Почта</label>
        <input
          required
          type="email"
          value={email}
          onChange={event => setEmail(event.target.value)}
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
          onChange={event => setPassword(event.target.value)}
          placeholder="Пароль_123"
          name='userPassword'
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
        <NavLink to="/password-reset" className="text-sm">Не помню пароль</NavLink>
        <LoginBtn />
      </form>
      <div className='text-primary flex gap-2 m-auto items-center justify-center'>
        <span className='text-center'>
          Нет аккаунта?
        </span>
        <button className='p-1 px-2 rounded-xl bg-secondary'>
          Зарегистрироваться
        </button>
      </div>
    </div>
  );
}