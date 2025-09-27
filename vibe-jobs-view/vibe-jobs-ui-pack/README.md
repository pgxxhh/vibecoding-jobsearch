
# Elaine Jobs — UI Pack

Drop-in styles + components to make the site cleaner, more "3D", and modern.

## What's inside
- `tailwind.extend.js` — Theme extension (brand colors, shadows, radii, animations)
- `styles/overrides.css` — Base CSS vars + background gradient + utility helpers
- `components/ui/*` — Button, Card, Badge, Input, Select, ToggleChip, Skeleton
- `components/JobCardNew.tsx` — Refined Job card
- `assets/*` — SVG hero/background orbs and a simple wordmark logo

## How to integrate (Next.js + Tailwind)
1. Copy everything into your project (merge folders).
2. Update `tailwind.config.js`:
   ```js
   module.exports = {
     content: ["./app/**/*.{js,ts,jsx,tsx}", "./components/**/*.{js,ts,jsx,tsx}"],
     theme: { extend: require('./tailwind.extend.js') },
     plugins: [],
   }
   ```
3. Import global overrides in your root layout or globals:
   ```ts
   import "@/styles/overrides.css";
   ```
4. (Optional) Switch fonts to Inter:
   ```ts
   import { Inter } from "next/font/google";
   const inter = Inter({ subsets: ["latin"] });
   <body className={inter.className}>...</body>
   ```
5. Use `<JobCardNew />` for the list.
6. Place the background SVG: 
   ```jsx
   <img src="/assets/hero-bg.svg" className="fixed inset-0 -z-10 w-full h-full object-cover" alt="" />
   ```
