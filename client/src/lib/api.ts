import axios from 'axios';

export const api = axios.create({
  baseURL: 'https://localhost:5173',
  headers: {
    'Content-Type': 'application/json',
  },
});