module.exports = {
  colors: {
    brand: {
      50:'#fff5fa',100:'#ffe4ef',200:'#ffc1da',300:'#ffa3c2',400:'#ff7fa6',
      500:'#f54d92',600:'#e13b7c',700:'#c81e5f',800:'#a80c47',900:'#7a032e'
    }
  },
  boxShadow: {
    'brand-xs':'0 1px 2px rgba(245,77,146,.06)',
    'brand-sm':'0 1px 3px rgba(245,77,146,.08), 0 1px 2px rgba(245,77,146,.06)',
    'brand-md':'0 8px 24px rgba(245,77,146,.08), 0 2px 8px rgba(245,77,146,.06)',
    'brand-lg':'0 12px 32px rgba(245,77,146,.10), 0 4px 12px rgba(245,77,146,.06)'
  },
  borderRadius: { 'xl':'0.875rem','2xl':'1.25rem','3xl':'1.75rem' },
  keyframes: {
    'fade-in': { from:{opacity:0, transform:'translateY(4px)'}, to:{opacity:1, transform:'translateY(0)'} },
    'pulse-soft': { '0%,100%':{opacity:.7}, '50%':{opacity:1} }
  },
  animation: { 'fade-in':'fade-in .25s ease-out both', 'pulse-soft':'pulse-soft 2s ease-in-out infinite' }
};
