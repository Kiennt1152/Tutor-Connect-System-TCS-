import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './shared/theme/tokens.css'
import HomePage from './features/home/pages/HomePage'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <HomePage />
  </StrictMode>,
)
