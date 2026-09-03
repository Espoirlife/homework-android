const ICONS = {
  back: '<path d="M20 11H7.8l5.6-5.6L12 4l-8 8 8 8 1.4-1.4L7.8 13H20v-2z"/>',
  close: '<path d="M19 6.4 17.6 5 12 10.6 6.4 5 5 6.4 10.6 12 5 17.6 6.4 19 12 13.4 17.6 19 19 17.6 13.4 12 19 6.4z"/>',
  arrowDown: '<path d="M7 10l5 5 5-5H7z"/>',
  more: '<path d="M12 8a2 2 0 100-4 2 2 0 000 4zm0 2a2 2 0 100 4 2 2 0 000-4zm0 6a2 2 0 100 4 2 2 0 000-4z"/>',
  qr: '<path d="M3 11h8V3H3v8zm2-6h4v4H5V5zM3 21h8v-8H3v8zm2-6h4v4H5v-4zM13 3v8h8V3h-8zm6 6h-4V5h4v4zm-6 6h2v2h-2v-2zm4 0h2v2h-2v-2zm-4 4h2v2h-2v-2zm4 0h2v2h-2v-2zm2-2h2v2h-2v-2z"/>',
  print: '<path d="M19 8H5a3 3 0 00-3 3v6h4v4h12v-4h4v-6a3 3 0 00-3-3zm-3 11H8v-5h8v5zm3-7a1 1 0 110-2 1 1 0 010 2zM18 3H6v4h12V3z"/>',
  check: '<path d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4L9 16.2z"/>',
  add: '<path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"/>',
  flip: '<path d="M20 5h-3.2l-1.8-2H9L7.2 5H4a2 2 0 00-2 2v11a2 2 0 002 2h16a2 2 0 002-2V7a2 2 0 00-2-2zm-8 12a4.5 4.5 0 01-4.4-3.6L6 15V9.8l1.4 1.4A4.5 4.5 0 0116.5 12h-2A2.5 2.5 0 0010 12l1.5 1.5H8.2A2.5 2.5 0 0012 15v2z"/>',
  tabClass: '<path d="M16 11a3 3 0 100-6 3 3 0 000 6zm-8 0a3 3 0 100-6 3 3 0 000 6zm0 2c-2.3 0-6 1.2-6 3.5V19h12v-2.5C14 14.2 10.3 13 8 13zm8 0c-.3 0-.6 0-1 .1 1.2.8 2 1.9 2 3.4V19h6v-2.5c0-2.3-3.7-3.5-7-3.5z"/>',
  tabHomework: '<path d="M17 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V5a2 2 0 00-2-2zm-6 14-3-3 1.4-1.4L11 14.2l4.6-4.6L17 11l-6 6zM8 6h8v2H8V6z"/>',
  tabReport: '<path d="M5 20h2v-8H5v8zm6 0h2V4h-2v16zm6 0h2v-5h-2v5z"/>',
  tabSettings: '<path d="M19.4 13a7.6 7.6 0 000-2l2-1.6-2-3.4-2.4 1a7.6 7.6 0 00-1.7-1L15 3.4h-4l-.3 2.6a7.6 7.6 0 00-1.7 1l-2.4-1-2 3.4L6.6 11a7.6 7.6 0 000 2l-2 1.6 2 3.4 2.4-1a7.6 7.6 0 001.7 1l.3 2.6h4l.3-2.6a7.6 7.6 0 001.7-1l2.4 1 2-3.4-2-1.6zM13 15.5a3.5 3.5 0 110-7 3.5 3.5 0 010 7z"/>',
  school: '<path d="M12 3 1 9l11 6 9-4.9V17h2V9L12 3zM5 13.2v3.3l7 3.8 7-3.8v-3.3l-7 3.8-7-3.8z"/>',
  file: '<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/>',
  folder: '<path d="M10 4H4a2 2 0 00-2 2v12a2 2 0 002 2h16a2 2 0 002-2V8a2 2 0 00-2-2h-8l-2-2z"/>',
  cloud: '<path d="M19.4 10a7.5 7.5 0 00-14-2A6 6 0 006 20h13a5 5 0 00.4-10z"/>',
  cloudUp: '<path d="M19.4 10a7.5 7.5 0 00-14-2A6 6 0 006 20h4v-4H7l5-5 5 5h-3v4h5a5 5 0 00.4-10z"/>',
  cloudDown: '<path d="M19.4 10a7.5 7.5 0 00-14-2A6 6 0 006 20h4v-4H7l5 5 5-5h-3v4h5a5 5 0 00.4-10z"/>',
  restore: '<path d="M13 3a9 9 0 00-9 9H1l4 4 4-4H6a7 7 0 117 7v2a9 9 0 000-18zm-1 5v5l4.3 2.5.7-1.2-3.5-2V8H12z"/>',
  eye: '<path d="M12 5C6.5 5 2.7 9.6 2 12c.7 2.4 4.5 7 10 7s9.3-4.6 10-7c-.7-2.4-4.5-7-10-7zm0 11a4 4 0 110-8 4 4 0 010 8zm0-6a2 2 0 100 4 2 2 0 000-4z"/>',
  trash: '<path d="M7 19a2 2 0 002 2h6a2 2 0 002-2V7H7v12zM18 4h-3l-1-1h-4l-1 1H6v2h12V4z"/>',
  chevronRight: '<path d="M9 6l6 6-6 6-1.4-1.4L12.2 12 7.6 7.4 9 6z"/>',
  info: '<path d="M12 2a10 10 0 100 20 10 10 0 000-20zm1 15h-2v-6h2v6zm0-8h-2V7h2v2z"/>',
  lock: '<path d="M18 8h-1V6a5 5 0 00-10 0v2H6a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V10a2 2 0 00-2-2zM9 6a3 3 0 016 0v2H9V6zm3 12a2 2 0 110-4 2 2 0 010 4z"/>',
  sync: '<path d="M12 4V1L8 5l4 4V6a6 6 0 015.2 9l1.5 1.5A8 8 0 0012 4zm0 14a6 6 0 01-5.2-9L5.3 7.5A8 8 0 0012 20v3l4-4-4-4v3z"/>'
};

const NAV_ITEMS = [
  { key: 'class', label: '班级', icon: 'tabClass' },
  { key: 'homework', label: '作业', icon: 'tabHomework' },
  { key: 'report', label: '报表', icon: 'tabReport' },
  { key: 'settings', label: '设置', icon: 'tabSettings' }
];

function svg(name) {
  return '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">' + ICONS[name] + '</svg>';
}

function renderIcons() {
  document.querySelectorAll('[data-icon]').forEach(function (el) {
    const name = el.dataset.icon;
    if (ICONS[name]) el.innerHTML = svg(name);
  });
}

function renderNavBars() {
  document.querySelectorAll('[data-nav]').forEach(function (bar) {
    const active = bar.dataset.nav;
    bar.className = 'nav-bar';
    bar.innerHTML = NAV_ITEMS.map(function (item) {
      const cls = item.key === active ? 'nav-item active' : 'nav-item';
      return '<div class="' + cls + '"><span class="nav-icon">' + svg(item.icon) + '</span>' + item.label + '</div>';
    }).join('');
  });
}

function renderStickers() {
  document.querySelectorAll('[data-stickers]').forEach(function (grid) {
    const names = JSON.parse(grid.dataset.stickers);
    grid.innerHTML = names.map(function (item, i) {
      const no = String(i + 1).padStart(2, '0');
      return '<div class="sticker"><div class="qr"></div><b>' + item + '</b><span>' + no + '</span></div>';
    }).join('');
  });
}

renderIcons();
renderNavBars();
renderStickers();
