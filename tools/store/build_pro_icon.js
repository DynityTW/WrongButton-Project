// 產生 Pro 商品圖示（512×512，32 位元 PNG，沒有文字）：戴皇冠的 The Button 在金色底上。
// 在專案根目錄執行：node tools/store/build_pro_icon.js，輸出 docs/store/graphics/pro_icon_512.png。
const { chromium } = require('playwright');
const INK='#2B1B18', BODY='#FF6B5B', SHADE='#C9473A';
(async () => {
  const b = await chromium.launch(); const p = await b.newPage({ viewport: { width: 512, height: 512 } });
  await p.setContent(`<body style="margin:0;background:transparent"><svg width="512" height="512" viewBox="0 0 108 108">
    <defs><radialGradient id="g" cx="50%" cy="40%" r="70%"><stop offset="0" stop-color="#FFE9A8"/><stop offset="1" stop-color="#F2B53A"/></radialGradient></defs>
    <rect width="108" height="108" rx="24" fill="url(#g)"/>
    <!-- 皇冠 -->
    <path d="M38 30 L44 18 L54 27 L64 18 L70 30 Z" fill="#FFF6D8" stroke="#B9791A" stroke-width="2" stroke-linejoin="round"/>
    <circle cx="44" cy="18" r="2.6" fill="#B9791A"/><circle cx="54" cy="25.5" r="2.6" fill="#B9791A"/><circle cx="64" cy="18" r="2.6" fill="#B9791A"/>
    <!-- The Button（得意的表情） -->
    <rect x="28" y="38" width="52" height="46" rx="15" fill="${SHADE}"/>
    <rect x="28" y="32" width="52" height="46" rx="15" fill="${BODY}"/>
    <rect x="36" y="35.5" width="36" height="5" rx="2.5" fill="#FFFFFF" opacity=".25"/>
    <circle cx="44" cy="51" r="7" fill="#FFFFFF"/><circle cx="64" cy="51" r="7" fill="#FFFFFF"/>
    <circle cx="44" cy="51" r="3.4" fill="${INK}"/><circle cx="64" cy="51" r="3.4" fill="${INK}"/>
    <path d="M44.5 63 Q54 71 63.5 63" stroke="${INK}" stroke-width="2.8" fill="none" stroke-linecap="round"/>
    <!-- 閃光 -->
    <path d="M86 40 l2 5 l5 2 l-5 2 l-2 5 l-2 -5 l-5 -2 l5 -2 Z" fill="#FFFFFF"/>
    <path d="M20 70 l1.4 3.4 l3.4 1.4 l-3.4 1.4 l-1.4 3.4 l-1.4 -3.4 l-3.4 -1.4 l3.4 -1.4 Z" fill="#FFFFFF"/>
  </svg></body>`);
  await p.screenshot({ path: 'docs/store/graphics/pro_icon_512.png', omitBackground: true }); await b.close();
})();
