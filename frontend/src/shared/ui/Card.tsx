import type { ReactNode } from 'react'
import styles from './Card.module.css'

interface CardProps {
  children: ReactNode
  title?: string
  className?: string
}

export function Card({ children, title, className }: CardProps) {
  return (
    <div className={`${styles.card} ${className || ''}`}>
      {title && <h2 className={styles.title}>{title}</h2>}
      {children}
    </div>
  )
}
