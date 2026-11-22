// 產生圖示與 Logo：商店圖示 512（淡珊瑚漸層）、深色備用圖示、橫式與方形 Logo（透明背景）。
// 在專案根目錄執行：node tools/store/build_logo.js（需要 Playwright、Roboto、Noto Sans CJK TC）。
// 輸出到 docs/store/graphics/。App 的桌面圖示是同一張圖：res/drawable/ic_launcher_*.xml。
const { chromium } = require('playwright');
const INK='#2B1B18', BODY='#FF6B5B', SHADE='#C9473A', PRIMARY='#B3402F';
const FONT = "font-family:'Roboto','Noto Sans CJK TC',sans-serif;";
const fg = `
  <rect x="28" y="34" width="52" height="48" rx="15" fill="${SHADE}"/>
  <rect x="28" y="28" width="52" height="48" rx="15" fill="${BODY}"/>
  <rect x="36" y="31.5" width="36" height="5.5" rx="2.75" fill="#FFFFFF" opacity=".25"/>
  <circle cx="44" cy="48" r="7" fill="#FFFFFF"/><circle cx="64" cy="48" r="7" fill="#FFFFFF"/>
  <circle cx="44" cy="48" r="3.4" fill="${INK}"/><circle cx="64" cy="48" r="3.4" fill="${INK}"/>
  <path d="M46.5 61 Q54 67 61.5 61" stroke="${INK}" stroke-width="2.8" fill="none" stroke-linecap="round"/>`;
const bgB = '<defs><radialGradient id="g" cx="50%" cy="38%" r="75%"><stop offset="0" stop-color="#FFF3EF"/><stop offset="1" stop-color="#FFC9BF"/></radialGradient></defs><rect x="-10" y="-10" width="128" height="128" fill="url(#g)"/>';
const bgC = '<rect x="-10" y="-10" width="128" height="128" fill="#2B1B18"/>';
(async () => {
  const b = await chromium.launch(); const p = await b.newPage();
  const icon = async (bg, file) => {
    await p.setViewportSize({ width: 512, height: 512 });
    await p.setContent(`<body style="margin:0"><svg width="512" height="512" viewBox="12 12 84 84">${bg}${fg}</svg></body>`);
    await p.screenshot({ path: file });
  };
  await icon(bgB, 'docs/store/graphics/icon_512.png');
  await icon(bgC, 'docs/store/graphics/icon_512_dark.png');
  // 橫式 Logo（透明背景）：角色＋字
  await p.setViewportSize({ width: 1200, height: 400 });
  await p.setContent(`<body style="margin:0;background:transparent"><div style="width:1200px;height:400px;display:flex;align-items:center;gap:36px;padding-left:60px;box-sizing:border-box;${FONT}">
    <svg width="300" height="300" viewBox="24 22 60 64">${fg}</svg>
    <div><div style="font-size:120px;font-weight:900;color:${INK};line-height:1">Wrong Button</div>
    <div style="font-size:52px;font-weight:700;color:${PRIMARY};margin-top:18px">一顆會說謊的按鈕。</div></div></div></body>`);
  await p.screenshot({ path: 'docs/store/graphics/logo_horizontal.png', omitBackground: true });
  // 方形 Logo（透明背景）
  await p.setViewportSize({ width: 1024, height: 1024 });
  await p.setContent(`<body style="margin:0;background:transparent"><div style="width:1024px;height:1024px;display:flex;flex-direction:column;align-items:center;justify-content:center;${FONT}">
    <svg width="560" height="560" viewBox="24 22 60 64">${fg}</svg>
    <div style="font-size:130px;font-weight:900;color:${INK};margin-top:24px">Wrong Button</div></div></body>`);
  await p.screenshot({ path: 'docs/store/graphics/logo_square.png', omitBackground: true });
  await b.close();
})();
