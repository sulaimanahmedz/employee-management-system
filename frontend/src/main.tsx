import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import AuthShell from './features/identity/AuthShell';
import './styles.css';

createRoot(document.getElementById('root')!).render(<StrictMode><AuthShell /></StrictMode>);
