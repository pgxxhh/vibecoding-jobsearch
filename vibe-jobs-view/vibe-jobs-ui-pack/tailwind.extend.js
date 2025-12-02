module.exports = {
  colors: {
    brand: {
      50:'#fff5f8',100:'#ffe0eb',200:'#ffc1d8',300:'#ffa3c2',400:'#ff7fa6',
      500:'#f54d92',600:'#e13b7c',700:'#c81e5f',800:'#a80c47',900:'#7a032e'
    }
  },
  boxShadow: {
    'brand-xs':'0 1px 2px rgba(245,77,146,.06)',
    'brand-sm':'0 1px 3px rgba(245,77,146,.08), 0 1px 2px rgba(245,77,146,.06)',
    'brand-md':'0 4px 16px rgba(245,77,146,.08), 0 2px 6px rgba(245,77,146,.04)',
    'brand-lg':'0 8px 30px rgba(245,77,146,.12), 0 4px 12px rgba(245,77,146,.06)',
    'brand-xl':'0 20px 40px rgba(245,77,146,.15), 0 8px 20px rgba(245,77,146,.08)',
    'glass':'0 4px 24px rgba(0,0,0,.03), 0 1px 2px rgba(0,0,0,.02)',
    'glass-lg':'0 8px 40px rgba(0,0,0,.06), 0 2px 8px rgba(0,0,0,.03)'
  },
  borderRadius: { 'xl':'0.875rem','2xl':'1.25rem','3xl':'1.75rem','4xl':'2rem' },
  keyframes: {
    'fade-in': { from:{opacity:0, transform:'translateY(8px)'}, to:{opacity:1, transform:'translateY(0)'} },
    'fade-in-up': { from:{opacity:0, transform:'translateY(16px)'}, to:{opacity:1, transform:'translateY(0)'} },
    'pulse-soft': { '0%,100%':{opacity:.7}, '50%':{opacity:1} },
    'float': { '0%,100%':{transform:'translateY(0)'}, '50%':{transform:'translateY(-6px)'} },
    'scale-in': { from:{opacity:0, transform:'scale(0.95)'}, to:{opacity:1, transform:'scale(1)'} }
  },
  animation: { 
    'fade-in':'fade-in .35s ease-out both', 
    'fade-in-up':'fade-in-up .5s ease-out both',
    'pulse-soft':'pulse-soft 2s ease-in-out infinite',
    'float':'float 6s ease-in-out infinite',
    'scale-in':'scale-in .25s ease-out both'
  },
  backgroundImage: {
    'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
    'gradient-conic': 'conic-gradient(from 180deg at 50% 50%, var(--tw-gradient-stops))',
  }
};
