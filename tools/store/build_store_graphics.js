// 產生 Google Play 商店圖片：7 張附標題的手機截圖（1080×1920）、1024×500 主圖片（圖示與 Logo 見 build_logo.js）。
//
// 1. 先截原始畫面（存到 app/build/store/）：
//      WB_STORE=1 WB_STORE_LEVELS=2,199,228,313 ./gradlew testDebugUnitTest --rerun --tests '*StoreScreenshotTool*'
//      WB_STORE=1 WB_STORE_LEVELS=6 ./gradlew testDebugUnitTest --rerun -Pwb.realMonetization=true --tests '*StoreScreenshotTool*'
//    （第二行截第 16 關〈顧問〉說謊的臉；要先備份第一行的結果，因為會覆蓋同一個資料夾。）
// 2. 在專案根目錄執行：node tools/store/build_store_graphics.js
//    需要 Playwright（Chromium），以及 Roboto、Noto Sans CJK TC 字型。
// 輸出到 docs/store/graphics/。
const { chromium } = require('playwright');
const fs = require('fs');
const FONT = `font-family: 'Roboto','Noto Sans CJK TC',sans-serif;`;
const BG = '#FFFBF8', PRIMARY = '#B3402F', BODY = '#FF6B5B', SHADE = '#C9473A', INK = '#2B1B18', CONT = '#FFDAD4';
const face = (lie, size) => `<svg width="${size}" height="${size}" viewBox="0 0 100 100">
  <rect x="8" y="14" width="84" height="80" rx="24" fill="${SHADE}"/><rect x="8" y="6" width="84" height="80" rx="24" fill="${BODY}"/>
  <rect x="22" y="12" width="56" height="9" rx="4.5" fill="#fff" opacity=".22"/>
  <circle cx="36" cy="42" r="11" fill="#fff"/><circle cx="64" cy="42" r="11" fill="#fff"/>
  <circle cx="${lie ? 41 : 36}" cy="${lie ? 40 : 42}" r="5.2" fill="${INK}"/><circle cx="${lie ? 69 : 64}" cy="${lie ? 40 : 42}" r="5.2" fill="${INK}"/>
  ${lie ? `<path d="M39 66 Q50 70 61 57" stroke="${INK}" stroke-width="3.6" fill="none" stroke-linecap="round"/>`
        : `<path d="M39 63 Q50 72 61 63" stroke="${INK}" stroke-width="3.6" fill="none" stroke-linecap="round"/>`}
</svg>`;
const shots = [
  ['title_new', '一顆會說謊的按鈕。', '任務永遠只有一句：按下按鈕。'],
  ['level_6', '它說謊時，眼睛會看旁邊。', '學會讀臉，你就不會再被騙。'],
  ['level_2', '按鈕不見了？', '下拉重新整理、返回鍵、溢位選單……都是解法。'],
  ['level_313', '500 關，30 章。', '第三季：踩地雷、我不是機器人、誰在說謊……'],
  ['level_228', 'App 的語言，就是謎題。', '影片章節、設定、檔案、試算表都藏著它。'],
  ['level_199', '換你出題給它玩。', '關卡編輯器、按鈕語、會說謊的新同事。'],
  ['records', '它記得你做過的每一件事。', '紀錄只存在你的手機裡。'],
];
(async () => {
  const b = await chromium.launch();
  const p = await b.newPage();
  // 1) 商店截圖 1080×1920
  for (const [name, title, sub] of shots) {
    // 原始截圖不在的就跳過（例如只想重做其中一張）。
    if (!fs.existsSync(`app/build/store/${name}.png`)) { console.log('略過', name); continue; }
    const img = 'data:image/png;base64,' + fs.readFileSync(`app/build/store/${name}.png`).toString('base64');
    await p.setViewportSize({ width: 1080, height: 1920 });
    await p.setContent(`<html lang="zh-Hant-TW"><body style="margin:0;width:1080px;height:1920px;background:linear-gradient(180deg,${CONT} 0%,${BG} 55%);${FONT}overflow:hidden">
      <div style="padding:84px 72px 0;text-align:center">
        <div style="font-size:74px;font-weight:900;color:${INK};letter-spacing:2px">${title}</div>
        <div style="font-size:40px;font-weight:700;color:${PRIMARY};margin-top:22px">${sub}</div>
      </div>
      <div style="position:absolute;left:50%;top:420px;transform:translateX(-50%);width:820px;height:1458px;border-radius:56px;overflow:hidden;
                  box-shadow:0 30px 80px rgba(120,40,30,.28);border:14px solid #2B1B18;background:#2B1B18">
        <img src="${img}" style="width:100%;height:100%;object-fit:cover;object-position:top;border-radius:42px;display:block">
      </div></body></html>`);
    await p.screenshot({ path: `docs/store/graphics/screenshot_${String(shots.findIndex(s => s[0] === name) + 1).padStart(2, '0')}_${name}.png` });
  }
  // 2) 圖示與 Logo 改由 tools/store/build_logo.js 產生。
  // 3) 主圖片 1024×500
  await p.setViewportSize({ width: 1024, height: 500 });
  await p.setContent(`<html lang="zh-Hant-TW"><body style="margin:0;width:1024px;height:500px;background:linear-gradient(120deg,${CONT},${BG} 70%);${FONT}overflow:hidden;position:relative">
    <div style="position:absolute;left:64px;top:128px">
      <div style="font-size:78px;font-weight:900;color:${INK};letter-spacing:0;line-height:1">Wrong Button</div>
      <div style="font-size:44px;font-weight:700;color:${PRIMARY};margin-top:28px">一顆會說謊的按鈕。</div>
      <div style="font-size:28px;color:${INK};opacity:.75;margin-top:22px">500 關解謎・單指・不限時</div>
    </div>
    <div style="position:absolute;right:230px;top:150px;transform:rotate(-6deg)">${face(false, 150)}</div>
    <div style="position:absolute;right:44px;top:112px;transform:rotate(7deg)">${face(true, 190)}</div>
    <div style="position:absolute;right:70px;top:330px;background:#fff;border-radius:22px;padding:12px 22px;font-size:26px;color:${INK};box-shadow:0 8px 24px rgba(0,0,0,.08)">「按我，我是對的。」</div>
  </body></html>`);
  await p.screenshot({ path: 'docs/store/graphics/feature_graphic_1024x500.png' });
  await b.close();
})();
