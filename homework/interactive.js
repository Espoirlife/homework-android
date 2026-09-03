(function () {
'use strict';

var KEY = 'hwt-proto-v1';

var COMPLETION = [
  { v: 'miss',    label: '\u2717', cls: 'miss',    text: '\u672a\u5b8c\u6210' },
  { v: 'done',    label: '\u2713', cls: 'done',    text: '\u5df2\u5b8c\u6210' },
  { v: 'partial', label: '\u534a',  cls: 'partial', text: '\u90e8\u5206\u5b8c\u6210' }
];

var CORRECTION = [
  { v: 'pending', label: '\u5f85',  cls: 'pending', text: '\u8bb2\u89e3\u540e\u8ba2\u6b63' },
  { v: 'fixed',   label: '\u2713', cls: 'done',    text: '\u8ba2\u6b63\u5b8c\u6210' }
];

var GRADES = [
  { v: '',  label: '\u2014', text: '\u672a\u8bc4' },
  { v: 'A', label: 'A', text: 'A' },
  { v: 'B', label: 'B', text: 'B' },
  { v: 'C', label: 'C', text: 'C' }
];

var COUNTED = { done: 1, partial: 1 };

var MARK_GROUPS = [
  { key: 'completion', label: '\u5b8c\u6210\u60c5\u51b5', list: COMPLETION },
  { key: 'correction', label: '\u8ba2\u6b63\u60c5\u51b5', list: CORRECTION },
  { key: 'grade',      label: '\u8bc4\u7ea7',         list: GRADES }
];

function find(list, v) {
  for (var i = 0; i < list.length; i++) if (list[i].v === v) return list[i];
  return list[0];
}

function next(list, v) {
  for (var i = 0; i < list.length; i++) if (list[i].v === v) return list[(i + 1) % list.length].v;
  return list[0].v;
}

function groupOf(key) {
  for (var i = 0; i < MARK_GROUPS.length; i++) if (MARK_GROUPS[i].key === key) return MARK_GROUPS[i];
  return null;
}

function esc(s) {
  return String(s == null ? '' : s).replace(/[&<>"]/g, function (c) {
    return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c];
  });
}

function pad(n, w) { return String(n).padStart(w, '0'); }

function uid(p) { return p + '-' + Math.random().toString(36).slice(2, 8); }

var DEMO_NAMES = [
  '\u5f20\u660e', '\u674e\u534e', '\u738b\u82b3', '\u9648\u4f1f', '\u5218\u6d0b', '\u8d75\u654f',
  '\u5b59\u6d69', '\u5468\u5a77', '\u5434\u78ca', '\u90d1\u723d', '\u9ec4\u6d9b', '\u5f90\u9759'
];

var DEMO_TITLES = [
  '\u7b2c\u4e00\u5355\u5143\u7ec3\u4e60', '\u8bfe\u540e\u4e60\u9898 P32', '\u53e3\u7b97\u5361 \u00d7 20',
  '\u4f5c\u6587\uff1a\u79cb\u5929', '\u5355\u5143\u590d\u4e60\u5377'
];

function seedStore() {
  var s = {
    onboarded: true,
    currentClassId: 'c-1',
    settings: {
      defaultCompletion: 'miss',
      defaultCorrection: 'pending',
      qrPerRow: 3,
      qrEcc: 'M',
      qrMargin: 2,
      qrWithNo: true,
      webdav: {
        enabled: true,
        url: 'https://dav.example.com/hwt/',
        user: 'teacher_wang',
        password: 'demo-pass',
        keep: 5,
        tested: 'ok'
      },
      autoBackup: true,
      lastBackupAt: '08-31 09:12'
    },
    classes: [],
    students: [],
    assignments: [],
    records: {},
    cloud: [
      { name: 'hwt-backup-20260831-0912.json', size: '128 KB', meta: '3 \u73ed / 126 \u4eba', latest: true },
      { name: 'hwt-backup-20260830-2041.json', size: '126 KB', meta: '3 \u73ed / 126 \u4eba' },
      { name: 'hwt-backup-20260829-1733.json', size: '124 KB', meta: '3 \u73ed / 125 \u4eba' },
      { name: 'hwt-backup-20260828-1902.json', size: '120 KB', meta: '2 \u73ed / 88 \u4eba' }
    ]
  };

  s.classes.push({ id: 'c-1', name: '\u4e09\u73ed\uff082\uff09', note: '2025 \u79cb\u5b63 \u00b7 \u6570\u5b66', prefix: '', digits: 2, recycle: false });
  s.classes.push({ id: 'c-2', name: '\u56db\u73ed\uff081\uff09', note: '', prefix: '', digits: 2, recycle: false });

  DEMO_NAMES.forEach(function (n, i) {
    s.students.push({ id: 'c-1-s' + (i + 1), classId: 'c-1', no: i + 1, name: n, note: i === 0 ? '\u6570\u5b66\u7ec4' : '' });
  });
  ['\u4f55\u5a1f', '\u6768\u5e06', '\u6797\u971e', '\u80e1\u5b87', '\u9a6c\u8d85', '\u7f57\u5a9b'].forEach(function (n, i) {
    s.students.push({ id: 'c-2-s' + (i + 1), classId: 'c-2', no: i + 1, name: n, note: '' });
  });

  DEMO_TITLES.forEach(function (t, i) {
    s.assignments.push({
      id: 'a-' + (i + 1),
      classId: 'c-1',
      title: t,
      date: '2026-08-' + pad(27 + i, 2),
      note: ''
    });
  });
  s.assignments.push({ id: 'a-6', classId: 'c-2', title: '\u62fc\u97f3\u7ec3\u4e60 3', date: '2026-08-31', note: '' });

  var seedRates = [0.80, 0.92, 0.75, 0.88, 0.87];
  s.assignments.slice(0, 5).forEach(function (a, ai) {
    var hit = Math.round(DEMO_NAMES.length * seedRates[ai]);
    DEMO_NAMES.forEach(function (_, si) {
      if (si >= hit && ai < 4) return;
      var sid = 'c-1-s' + (si + 1);
      var comp = si < hit ? (si % 7 === 3 ? 'partial' : 'done') : 'miss';
      s.records[a.id + '|' + sid] = {
        completion: comp,
        correction: comp === 'done' ? 'fixed' : 'pending',
        grade: si % 3 === 0 ? 'A' : (si % 3 === 1 ? 'B' : '')
      };
    });
  });

  return s;
}

function emptyStore() {
  var s = seedStore();
  s.onboarded = false;
  s.classes = [];
  s.students = [];
  s.assignments = [];
  s.records = {};
  s.currentClassId = null;
  s.cloud = [];
  s.settings.webdav.enabled = false;
  s.settings.webdav.url = '';
  s.settings.webdav.user = '';
  s.settings.webdav.password = '';
  s.settings.webdav.tested = null;
  s.settings.lastBackupAt = null;
  return s;
}

var store;

var COMP_MIGRATE = { late: 'done' };
var CORR_MIGRATE = { none: 'fixed', failed: 'pending' };

function migrate(s) {
  if (!s) return s;
  if (s.settings) {
    if (COMP_MIGRATE[s.settings.defaultCompletion]) s.settings.defaultCompletion = COMP_MIGRATE[s.settings.defaultCompletion];
    if (CORR_MIGRATE[s.settings.defaultCorrection]) s.settings.defaultCorrection = CORR_MIGRATE[s.settings.defaultCorrection];
  }
  var recs = s.records || {};
  Object.keys(recs).forEach(function (k) {
    var r = recs[k];
    if (!r) return;
    if (COMP_MIGRATE[r.completion]) r.completion = COMP_MIGRATE[r.completion];
    if (CORR_MIGRATE[r.correction]) r.correction = CORR_MIGRATE[r.correction];
  });
  return s;
}

function load() {
  try {
    var raw = localStorage.getItem(KEY);
    if (raw) { store = migrate(JSON.parse(raw)); save(); return; }
  } catch (e) {}
  store = seedStore();
  save();
}

function save() {
  try { localStorage.setItem(KEY, JSON.stringify(store)); } catch (e) {}
}

/* ---------- 派生数据 ---------- */

function currentClass() {
  for (var i = 0; i < store.classes.length; i++) {
    if (store.classes[i].id === store.currentClassId) return store.classes[i];
  }
  return store.classes[0] || null;
}

function classById(id) {
  for (var i = 0; i < store.classes.length; i++) {
    if (store.classes[i].id === id) return store.classes[i];
  }
  return null;
}

function studentById(id) {
  for (var i = 0; i < store.students.length; i++) {
    if (store.students[i].id === id) return store.students[i];
  }
  return null;
}

function studentsOf(classId) {
  return store.students.filter(function (s) { return s.classId === classId; })
    .sort(function (a, b) { return a.no - b.no; });
}

function assignmentsOf(classId) {
  return store.assignments.filter(function (a) { return a.classId === classId; })
    .sort(function (a, b) { return a.date < b.date ? 1 : -1; });
}

function currentAssignment() {
  var list = assignmentsOf(store.currentClassId);
  var hit = null;
  list.forEach(function (a) { if (a.id === ui.reportAid) hit = a; });
  return hit || list[0] || null;
}

function recordOf(assignmentId, studentId) {
  var r = store.records[assignmentId + '|' + studentId];
  if (r) return { completion: r.completion, correction: r.correction, grade: r.grade, saved: true };
  return {
    completion: store.settings.defaultCompletion,
    correction: store.settings.defaultCorrection,
    grade: '',
    saved: false
  };
}

function writeRecord(assignmentId, studentId, patch) {
  var key = assignmentId + '|' + studentId;
  var cur = recordOf(assignmentId, studentId);
  store.records[key] = {
    completion: patch.completion != null ? patch.completion : cur.completion,
    correction: patch.correction != null ? patch.correction : cur.correction,
    grade: patch.grade != null ? patch.grade : cur.grade
  };
  save();
}

function statsOf(assignmentId, classId) {
  var list = studentsOf(classId);
  var out = { total: list.length, done: 0, miss: 0, partial: 0, pending: 0, counted: 0, rate: 0 };
  list.forEach(function (s) {
    var r = recordOf(assignmentId, s.id);
    out[r.completion] = (out[r.completion] || 0) + 1;
    if (COUNTED[r.completion]) out.counted++;
    if (r.correction === 'pending') out.pending++;
  });
  out.rate = out.total ? Math.round(out.counted * 100 / out.total) : 0;
  return out;
}

function classSummary(classId) {
  var list = assignmentsOf(classId);
  var recent = list.slice(0, 5).reverse();
  var bars = recent.map(function (a) {
    var st = statsOf(a.id, classId);
    return { id: a.id, title: a.title, date: a.date, rate: st.rate };
  });
  var avg = bars.length
    ? Math.round(bars.reduce(function (n, b) { return n + b.rate; }, 0) / bars.length)
    : 0;
  var pending = 0, missTotal = 0;
  list.forEach(function (a) {
    var st = statsOf(a.id, classId);
    pending += st.pending;
    missTotal += st.miss;
  });
  return { bars: bars, avg: avg, pending: pending, missTotal: missTotal, count: list.length };
}

function nextNo(cls) {
  var list = studentsOf(cls.id);
  if (cls.recycle) {
    for (var n = 1; n <= list.length + 1; n++) {
      var used = list.some(function (s) { return s.no === n; });
      if (!used) return n;
    }
  }
  return list.reduce(function (m, s) { return Math.max(m, s.no); }, 0) + 1;
}

function noText(cls, student) {
  return (cls.prefix || '') + pad(student.no, cls.digits || 2);
}

/* ---------- 界面状态与路由 ---------- */

var ui = {
  classTab: 'class', reportTab: 'class', personId: null, reportAid: null, expandedClassId: null,
  pwVisible: false, wizard: null, scanLog: [], dialog: null,
  clsRule: { recycle: false },
  bulkPick: { completion: null, correction: null, grade: null },
  scanPick: { completion: 'done', correction: null, grade: null }
};

var route = { name: 'homework', params: {} };
var stack = [], goingBack = false, lastRoute = null, scrollMem = {};

function go(name, params) {
  stack.push({ name: route.name, params: route.params });
  route = { name: name, params: params || {} };
  goingBack = false;
  render();
}

function goTab(name) {
  stack = [];
  route = { name: name, params: {} };
  goingBack = false;
  render();
}

function replace(name, params) {
  route = { name: name, params: params || {} };
  goingBack = false;
  render();
}

function back() {
  var prev = stack.pop();
  if (!prev) { goTab('homework'); return; }
  route = prev;
  goingBack = true;
  render();
}

var SCREENS = {};
var SCROLLER = '.body, .scan-body, .onboard';

function render() {
  var host = document.getElementById('screen');
  var old = host.querySelector(SCROLLER);
  if (old && lastRoute) scrollMem[lastRoute] = old.scrollTop;
  var def = SCREENS[route.name] || SCREENS.homework;
  host.className = '';
  void host.offsetHeight;
  host.innerHTML = def.html(route.params);
  host.className = 'screen-root' + (goingBack ? ' back' : '');
  document.getElementById('phone').classList.toggle('dark', !!def.dark);
  var lbl = document.getElementById('route-label');
  if (lbl) lbl.textContent = def.label ? def.label(route.params) : route.name;
  renderIcons();
  var now = host.querySelector(SCROLLER);
  if (now && scrollMem[route.name]) now.scrollTop = scrollMem[route.name];
  lastRoute = route.name;
  renderDialog();
}

/* ---------- 通用片段 ---------- */

function navHtml(active) {
  return '<nav class="nav-bar">' + NAV_ITEMS.map(function (it) {
    return '<div class="nav-item' + (it.key === active ? ' active' : '') +
      '" data-act="tab" data-tab="' + it.key + '">' +
      '<span class="nav-icon">' + svg(it.icon) + '</span>' + it.label + '</div>';
  }).join('') + '</nav>';
}

function classSelectHtml() {
  var c = currentClass();
  if (!c) return '';
  return '<span class="class-select" data-act="pick-class">' + esc(c.name) +
    '<span data-icon="arrowDown"></span></span>';
}

function backBtn() {
  return '<button class="icon-btn" data-act="back" data-icon="back" aria-label="\u8fd4\u56de"></button>';
}

function emptyHtml(icon, title, sub) {
  return '<div class="empty"><span class="em-icon" data-icon="' + icon + '"></span>' +
    '<b>' + esc(title) + '</b><span>' + esc(sub) + '</span></div>';
}

function switchHtml(on, act, extra) {
  return '<span class="switch' + (on ? '' : ' off') + '" data-act="' + act + '"' +
    (extra || '') + '></span>';
}

function rowField(label, valueHtml) {
  return '<div class="row-field"><span class="label">' + esc(label) + '</span>' + valueHtml + '</div>';
}

function markGroupsHtml(pick, act) {
  return MARK_GROUPS.map(function (g) {
    return '<div class="mark-group"><div class="mg-label">' + esc(g.label) + '</div>' +
      '<div class="chip-row tight">' + g.list.map(function (o) {
        var on = pick[g.key] === o.v;
        return '<button class="chip' + (on ? ' selected' : '') + '" data-act="' + act +
          '" data-g="' + g.key + '" data-v="' + esc(o.v) + '">' +
          (on ? '<span data-icon="check"></span>' : '') + esc(o.text) + '</button>';
      }).join('') + '</div></div>';
  }).join('');
}

function togglePick(pick, key, v) {
  pick[key] = pick[key] === v ? null : v;
}

function markHtml(act, key, sid, opt) {
  return '<span class="mark ' + (opt.cls || '') + '" data-act="' + act +
    '" data-long="mark-pick" data-g="' + key + '" data-sid="' + sid +
    '" title="' + esc(opt.text + '\uff08\u5355\u51fb\u5faa\u73af \u00b7 \u957f\u6309\u76f4\u9009\uff09') + '">' +
    opt.label + '</span>';
}

function openMarkPicker(key, sid) {
  var g = groupOf(key), aid = route.params.id;
  if (!g || !sid || !aid) return;
  var s = studentById(sid);
  var r = recordOf(aid, sid);
  openDialog({
    title: (s ? s.name : '') + ' \u00b7 ' + g.label,
    act: 'mark-set',
    group: key,
    sid: sid,
    value: r[key],
    options: g.list.map(function (o) {
      return { v: o.v, label: o.label === o.text ? o.text : o.label + '\u3000' + o.text };
    }),
    cancel: '\u5173\u95ed'
  });
}

function hintLongPress() {
  if (store.settings.longPressHinted) return;
  ui.cycCount = (ui.cycCount || 0) + 1;
  if (ui.cycCount < 3) return;
  store.settings.longPressHinted = true;
  save();
  toast('\u63d0\u793a\uff1a\u957f\u6309\u72b6\u6001\u6807\u53ef\u76f4\u63a5\u9009\u62e9\uff0c\u4e0d\u5fc5\u9010\u6b21\u5faa\u73af');
}

function pickPatch(pick) {
  var patch = {}, n = 0;
  MARK_GROUPS.forEach(function (g) {
    if (pick[g.key] !== null && pick[g.key] !== undefined) { patch[g.key] = pick[g.key]; n++; }
  });
  return n ? patch : null;
}

function pickSummary(pick) {
  var parts = [];
  MARK_GROUPS.forEach(function (g) {
    if (pick[g.key] !== null && pick[g.key] !== undefined) parts.push(find(g.list, pick[g.key]).text);
  });
  return parts.join(' \u00b7 ');
}

function selectHtml(text, act, extra) {
  return '<span class="select" data-act="' + act + '"' + (extra || '') + '>' + esc(text) +
    '<span data-icon="arrowDown"></span></span>';
}

function navRow(icon, title, sub, act, extra) {
  return '<div class="nav-row" data-act="' + act + '"' + (extra || '') + '>' +
    '<span class="avatar" data-icon="' + icon + '"></span>' +
    '<div class="nr-text"><div class="nr-title">' + esc(title) + '</div>' +
    (sub ? '<div class="nr-sub">' + esc(sub) + '</div>' : '') + '</div>' +
    '<span data-icon="chevronRight"></span></div>';
}

/* ---------- 提示与对话框 ---------- */

var toastTimer = null;

function toast(msg) {
  var el = document.getElementById('toast');
  el.textContent = msg;
  el.classList.add('show');
  if (toastTimer) clearTimeout(toastTimer);
  toastTimer = setTimeout(function () { el.classList.remove('show'); }, 1800);
}

function openDialog(opts) { ui.dialog = opts; renderDialog(); }
function closeDialog() { ui.dialog = null; renderDialog(); }

function renderDialog() {
  var host = document.getElementById('dialog-host');
  var d = ui.dialog;
  if (!d) { host.innerHTML = ''; return; }
  var body = '';
  if (d.menu) {
    body = '<div class="dlg-menu">' + d.menu.map(function (m, i) {
      return '<div class="dlg-mi' + (m.danger ? ' danger' : '') +
        '" data-act="dlg-menu" data-i="' + i + '">' +
        '<span data-icon="' + (m.icon || 'chevronRight') + '"></span>' +
        '<div class="dlg-mi-text"><div class="dlg-mi-title">' + esc(m.label) + '</div>' +
        (m.sub ? '<div class="dlg-mi-sub">' + esc(m.sub) + '</div>' : '') +
        '</div></div>';
    }).join('') + '</div>';
  } else if (d.options) {
    body = '<div class="dlg-list">' + d.options.map(function (o, i) {
      return '<div class="dlg-opt' + (o.v === d.value ? ' sel' : '') +
        '" data-act="dlg-pick" data-i="' + i + '"><i></i><span>' + esc(o.label) + '</span></div>';
    }).join('') + '</div>';
  } else if (d.text) {
    body = '<p>' + esc(d.text) + '</p>';
  } else if (d.form) {
    body = d.form;
  } else if (d.input != null) {
    body = '<div class="field" style="padding-top:0">' +
      '<div class="input"><input data-field="dlg-input" value="' + esc(d.input) +
      '" placeholder="' + esc(d.placeholder || '') + '"></div>' +
      (d.hint ? '<div class="field-hint">' + esc(d.hint) + '</div>' : '') + '</div>';
  }
  var actions = '<div class="dlg-actions">' +
    '<button class="btn text" data-act="dlg-cancel">' + esc(d.cancel || '\u53d6\u6d88') + '</button>' +
    (d.confirm ? '<button class="btn filled" data-act="dlg-ok">' + esc(d.confirm) + '</button>' : '') +
    '</div>';
  host.innerHTML = '<div class="scrim" data-act="dlg-cancel"><div class="dialog" data-stop="1">' +
    '<h3>' + esc(d.title) + '</h3>' + body + actions + '</div></div>';
  renderIcons();
}

/* ---------- 屏：引导页 ---------- */

var draft = { name: '', note: '', prefix: '', digits: 2, recycle: false };

SCREENS.onboard = {
  label: function () { return '\u5f15\u5bfc\u9875 \u00b7 FR-1.1~1.3'; },
  html: function () {
    var sample = [1, 2, 3].map(function (n) {
      return (draft.prefix || '') + pad(n, draft.digits);
    }).join('\u3001');
    return '<div class="onboard">' +
      '<div class="onboard-badge" data-icon="school"></div>' +
      '<h2>\u5148\u5efa\u4e00\u4e2a\u73ed\u7ea7</h2>' +
      '<p>\u5efa\u597d\u73ed\u7ea7\u540e\u5c31\u80fd\u5bfc\u5165\u5b66\u751f\u540d\u5355\u3001\u5e03\u7f6e\u4f5c\u4e1a\u3002\u5b66\u53f7\u4f1a\u6309\u4e0b\u9762\u7684\u89c4\u5219\u81ea\u52a8\u7f16\u6392\uff0c\u4e4b\u540e\u53ef\u4ee5\u968f\u65f6\u4fee\u6539\u3002</p>' +
      '<div class="field" style="padding-left:0;padding-right:0"><label>\u73ed\u7ea7\u540d\u79f0</label>' +
      '<div class="input"><input data-field="name" value="' + esc(draft.name) + '" placeholder="\u4f8b\uff1a\u4e09\u73ed\uff082\uff09"></div></div>' +
      '<div class="field" style="padding-left:0;padding-right:0"><label>\u5907\u6ce8\uff08\u53ef\u9009\uff09</label>' +
      '<div class="input"><input data-field="note" value="' + esc(draft.note) + '" placeholder="\u4f8b\uff1a2025 \u79cb\u5b63 \u00b7 \u6570\u5b66"></div></div>' +
      '<div class="field" style="padding-left:0;padding-right:0"><label>\u5b66\u53f7\u7f16\u53f7\u89c4\u5219</label>' +
      '<div class="field-2col"><div><div class="input"><input data-field="prefix" value="' + esc(draft.prefix) + '" placeholder="\u524d\u7f00\uff08\u53ef\u7a7a\uff09"></div></div>' +
      '<div><div class="input"><input data-field="digits" type="number" min="1" max="4" value="' + draft.digits + '"></div></div></div>' +
      '<div class="field-hint">\u793a\u4f8b\uff1a' + esc(sample) + '\u2026</div></div>' +
      '<div class="row-field" style="padding-left:0;padding-right:0"><span class="label">\u56de\u6536\u7a7a\u53f7</span>' +
      switchHtml(draft.recycle, 'draft-recycle') + '</div>' +
      '<div class="onboard-foot"><button class="btn filled tall" style="width:100%" data-act="create-class">\u521b\u5efa\u5e76\u5f00\u59cb</button></div>' +
      '</div>';
  }
};

/* ---------- 屏：班级与学生 ---------- */

SCREENS['class'] = {
  label: function () { return 'Tab 1 \u00b7 \u73ed\u7ea7\u4e0e\u5b66\u751f'; },
  html: function () {
    var body;
    var bodyCls = ui.classTab === 'class' ? 'body has-fab' : 'body';
    if (!store.classes.length) {
      body = '<div class="' + bodyCls + '">' + emptyHtml('school', '\u8fd8\u6ca1\u6709\u73ed\u7ea7', '\u70b9\u53f3\u4e0b\u89d2\u65b0\u5efa\u4e00\u4e2a\u73ed\u7ea7\u5f00\u59cb\u4f7f\u7528') + '</div>';
    } else if (ui.classTab === 'class') {
      body = '<div class="' + bodyCls + '">' + store.classes.map(function (c) {
        var open = c.id === ui.expandedClassId;
        var sts = studentsOf(c.id);
        var head = '<div class="card-head tappable" data-act="open-class" data-long="class-edit" data-id="' + c.id + '">' +
          '<div class="li-text"><div class="li-title">' + esc(c.name) + '</div>' +
          '<div class="li-sub">' + sts.length + ' \u540d\u5b66\u751f \u00b7 ' + assignmentsOf(c.id).length + ' \u4efd\u4f5c\u4e1a</div></div>' +
          '<span class="card-arrow' + (open ? ' open' : '') + '" data-icon="arrowDown"></span></div>';
        var rows = '';
        if (open) {
          rows = '<div class="card-divider"></div>' + (sts.length
            ? sts.slice(0, 6).map(function (s) {
                return '<div class="list-item tappable" data-act="student-menu" data-id="' + s.id + '">' +
                  '<span class="avatar">' + esc(noText(c, s)) + '</span>' +
                  '<div class="li-text"><div class="li-title">' + esc(s.name) + '</div>' +
                  (s.note ? '<div class="li-sub">' + esc(s.note) + '</div>' : '') + '</div>' +
                  '<span data-icon="chevronRight"></span></div>';
              }).join('') + (sts.length > 6
                ? '<div class="list-item tappable" data-act="class-tab" data-v="students"><div class="li-text"><div class="li-sub">\u5171 ' + sts.length + ' \u4eba\uff0c\u5207\u5230\u300c\u5b66\u751f\u300d\u9875\u67e5\u770b\u5168\u90e8</div></div></div>'
                : '')
            : '<div class="list-item"><div class="li-text"><div class="li-sub">\u8fd8\u6ca1\u6709\u5b66\u751f\uff0c\u53ef\u4ee5\u4ece Excel \u5bfc\u5165</div></div>' +
              '<button class="btn text" data-act="wizard-start">\u5bfc\u5165\u540d\u5355</button></div>');
        }
        return '<div class="card">' + head + rows + '</div>';
      }).join('') + '</div>';
    } else {
      var c = currentClass();
      var list = studentsOf(c.id);
      body = '<div class="body">' + (list.length
        ? '<div class="section-title">' + esc(c.name) + ' \u00b7 ' + list.length + ' \u4eba</div>' +
          list.map(function (s) {
            return '<div class="list-item compact tappable" data-act="student-menu" data-id="' + s.id + '">' +
              '<span class="avatar">' + esc(noText(c, s)) + '</span>' +
              '<div class="li-text"><div class="li-title">' + esc(s.name) + '</div>' +
              (s.note ? '<div class="li-sub">' + esc(s.note) + '</div>' : '') + '</div>' +
              '<span data-icon="chevronRight"></span></div>';
          }).join('')
        : emptyHtml('tabClass', '\u540d\u5355\u662f\u7a7a\u7684', '\u53ef\u4ee5\u4ece Excel \u5bfc\u5165\uff0c\u6216\u9010\u4e2a\u6dfb\u52a0')) +
        '<div class="btn-row"><button class="btn outlined" data-act="wizard-start">Excel \u5bfc\u5165</button>' +
        '<button class="btn filled" data-act="add-student">\u6dfb\u52a0\u5b66\u751f</button></div></div>';
    }
    var fab = ui.classTab === 'class'
      ? '<button class="fab extended" data-act="add-class"><span data-icon="add"></span>\u65b0\u5efa\u73ed\u7ea7</button>'
      : '';
    return '<div class="top-bar"><div class="top-bar-title" style="padding-left:16px">\u73ed\u7ea7\u4e0e\u5b66\u751f</div></div>' +
      '<div class="tab-row"><div class="tab' + (ui.classTab === 'class' ? ' active' : '') + '" data-act="class-tab" data-v="class">\u73ed\u7ea7</div>' +
      '<div class="tab' + (ui.classTab === 'students' ? ' active' : '') + '" data-act="class-tab" data-v="students">\u5b66\u751f</div></div>' +
      body + fab + navHtml('class');
  }
};

/* ---------- 屏：作业列表 ---------- */

SCREENS.homework = {
  label: function () { return 'Tab 2 \u00b7 \u4f5c\u4e1a\u7ba1\u7406'; },
  html: function () {
    var c = currentClass();
    if (!c) {
      return '<div class="top-bar"><div class="top-bar-title" style="padding-left:16px">\u4f5c\u4e1a</div></div>' +
        '<div class="body">' + emptyHtml('school', '\u8fd8\u6ca1\u6709\u73ed\u7ea7', '\u5148\u5230\u300c\u73ed\u7ea7\u300d\u9875\u65b0\u5efa\u4e00\u4e2a\u73ed\u7ea7') + '</div>' + navHtml('homework');
    }
    var list = assignmentsOf(c.id);
    var body = list.length
      ? '<div class="section-title">\u5168\u90e8\u4f5c\u4e1a \u00b7 ' + list.length + ' \u4efd</div>' + list.map(function (a) {
          var st = statsOf(a.id, c.id);
          return '<div class="list-item tappable" data-act="open-assignment" data-id="' + a.id + '">' +
            '<div class="li-text"><div class="li-title">' + esc(a.title) + '</div>' +
            '<div class="li-sub">' + esc(a.date.slice(5).replace('-', '\u6708') + '\u65e5') +
            ' \u00b7 \u5b8c\u6210\u7387 ' + st.rate + '% \u00b7 \u5f85\u8bb2\u89e3 ' + st.pending + '</div></div>' +
            '<span class="pill-count">' + st.counted + '/' + st.total + '</span>' +
            '<span data-icon="chevronRight"></span></div>';
        }).join('')
      : emptyHtml('tabHomework', '\u8fd8\u6ca1\u6709\u4f5c\u4e1a', '\u70b9\u53f3\u4e0b\u89d2\u5e03\u7f6e\u7b2c\u4e00\u4efd\u4f5c\u4e1a');
    return '<div class="top-bar"><div class="top-bar-title" style="padding-left:16px">\u4f5c\u4e1a</div>' +
      classSelectHtml() + '</div><div class="body has-fab">' + body + '</div>' +
      '<button class="fab extended" data-act="add-assignment"><span data-icon="add"></span>\u5e03\u7f6e\u4f5c\u4e1a</button>' +
      navHtml('homework');
  }
};

/* ---------- 屏：作业录入页 ---------- */

SCREENS.entry = {
  label: function (p) { return '\u5f55\u5165\u9875 \u00b7 FR-5 \u00b7 ' + p.id; },
  html: function (p) {
    var a = null;
    store.assignments.forEach(function (x) { if (x.id === p.id) a = x; });
    if (!a) return SCREENS.homework.html();
    var c = null;
    store.classes.forEach(function (x) { if (x.id === a.classId) c = x; });
    var list = studentsOf(a.classId);
    var st = statsOf(a.id, a.classId);
    var rows = list.map(function (s) {
      var r = recordOf(a.id, s.id);
      var comp = find(COMPLETION, r.completion);
      var corr = find(CORRECTION, r.correction);
      var grade = find(GRADES, r.grade);
      var sub = r.saved ? comp.text + ' \u00b7 ' + corr.text : '\u5c1a\u672a\u8bb0\u5f55';
      return '<div class="list-item"><span class="avatar">' + esc(noText(c, s)) + '</span>' +
        '<div class="li-text"><div class="li-title">' + esc(s.name) + '</div>' +
        '<div class="li-sub">' + esc(sub) + '</div></div>' +
        '<div class="marks">' +
        markHtml('cyc-comp', 'completion', s.id, comp) +
        markHtml('cyc-corr', 'correction', s.id, corr) +
        markHtml('cyc-grade', 'grade', s.id, grade) +
        '</div></div>';
    }).join('');
    var bulkSum = pickSummary(ui.bulkPick);
    return '<div class="top-bar">' + backBtn() +
      '<div class="top-bar-title">' + esc(a.title) + '</div>' + classSelectHtml() + '</div>' +
      '<div class="body has-fab">' +
      '<div class="section-title">\u6279\u91cf\u8bbe\u7f6e \u00b7 \u53ef\u591a\u7ef4\u5ea6\u540c\u65f6\u9009</div>' +
      '<div class="bulk-panel">' +
      markGroupsHtml(ui.bulkPick, 'bulk-pick') +
      '<div class="bulk-bar">' +
      '<span class="bulk-sum">' + (bulkSum ? esc('\u5c06\u6279\u91cf\u8bbe\u4e3a\uff1a' + bulkSum) : '\u672a\u9009\u62e9\u4efb\u4f55\u9879') + '</span>' +
      '<div class="bulk-btns">' +
      '<button class="btn text" data-act="bulk-clear">\u6e05\u7a7a\u9009\u62e9</button>' +
      '<button class="btn filled' + (bulkSum ? '' : ' disabled') + '" data-act="bulk-apply">\u5e94\u7528\u5230\u5168\u73ed ' + st.total + ' \u4eba</button>' +
      '</div></div></div>' +
      '<div class="stat-row">' +
      '<div class="stat"><b>' + st.rate + '%</b><span>\u5b8c\u6210\u7387</span></div>' +
      '<div class="stat"><b>' + st.pending + '</b><span>\u5f85\u8bb2\u89e3</span></div>' +
      '<div class="stat"><b>' + st.miss + '</b><span>\u672a\u5b8c\u6210</span></div></div>' +
      '<div class="section-title">\u5b66\u751f\u8bb0\u5f55 \u00b7 ' + esc(a.date.slice(5).replace('-', '\u6708') + '\u65e5') + '</div>' +
      rows + '</div>' +
      '<button class="fab" data-act="open-scan" data-icon="qr" aria-label="\u626b\u7801\u5f55\u5165"></button>' +
      navHtml('homework');
  }
};

/* ---------- 屏：扫码模式 ---------- */

SCREENS.scan = {
  dark: true,
  label: function () { return '\u626b\u7801\u6a21\u5f0f \u00b7 \u00a711 CAMERA'; },
  html: function (p) {
    var sum = pickSummary(ui.scanPick);
    var lastLog = ui.scanLog.length
      ? '<div class="scan-toast"><span data-icon="check"></span>' + esc('\u5df2\u8bb0\u5f55\uff1a' + ui.scanLog[ui.scanLog.length - 1]) + '</div>'
      : '<div class="scan-toast">\u5bf9\u51c6\u5b66\u751f\u8d34\u7eb8\u4e0a\u7684\u4e8c\u7ef4\u7801</div>';
    return '<div class="top-bar"><button class="icon-btn" data-act="back" data-icon="close" aria-label="\u5173\u95ed"></button>' +
      '<div class="top-bar-title">\u626b\u7801\u5f55\u5165</div></div>' +
      '<div class="scan-body">' +
      '<div class="scan-hint">\u626b\u5230\u5373\u6807\u8bb0\u4e3a\uff1a' +
      (sum ? esc(sum) : '\u8bf7\u81f3\u5c11\u9009\u4e00\u9879') + '</div>' +
      '<div class="scan-picks">' + markGroupsHtml(ui.scanPick, 'scan-pick') + '</div>' +
      '<div class="viewfinder" data-act="scan-hit" title="\u70b9\u51fb\u53d6\u666f\u6846\u6a21\u62df\u626b\u4e00\u4e2a\u5b66\u751f">' +
      '<span class="corner tl"></span><span class="corner tr"></span>' +
      '<span class="corner bl"></span><span class="corner br"></span></div>' +
      lastLog +
      '<div class="scan-actions"><button class="btn filled tall" data-act="back">\u505c\u6b62\u626b\u7801</button>' +
      '<button class="icon-btn" data-act="noop" data-icon="flip" aria-label="\u5207\u6362\u6444\u50cf\u5934"></button></div></div>';
  }
};

/* ---------- 屏：统计报表 ---------- */

SCREENS.report = {
  label: function () { return 'Tab 3 \u00b7 \u7edf\u8ba1\u62a5\u8868 \u00b7 FR-8.3'; },
  html: function () {
    var c = currentClass();
    if (!c) {
      return '<div class="top-bar"><div class="top-bar-title" style="padding-left:16px">\u7edf\u8ba1\u62a5\u8868</div></div>' +
        '<div class="body">' + emptyHtml('tabReport', '\u6ca1\u6709\u53ef\u7edf\u8ba1\u7684\u6570\u636e', '\u5efa\u73ed\u7ea7\u3001\u5bfc\u5165\u540d\u5355\u540e\u518d\u6765\u770b') + '</div>' + navHtml('report');
    }
    var body;
    if (ui.reportTab === 'class') {
      var sum = classSummary(c.id);
      var missRows = [];
      assignmentsOf(c.id).slice(0, 2).forEach(function (a) {
        studentsOf(c.id).forEach(function (s) {
          if (missRows.length >= 6) return;
          if (recordOf(a.id, s.id).completion === 'miss') {
            missRows.push('<div class="list-item compact tappable" data-act="open-person" data-id="' + s.id + '">' +
              '<span class="avatar">' + esc(noText(c, s)) + '</span>' +
              '<div class="li-text"><div class="li-title">' + esc(s.name) + '</div>' +
              '<div class="li-sub">' + esc(a.title) + '</div></div>' +
              '<span class="mark miss">\u2717</span></div>');
          }
        });
      });
      body = '<div class="stat-row">' +
        '<div class="stat"><b>' + sum.avg + '%</b><span>\u5b8c\u6210\u7387</span></div>' +
        '<div class="stat"><b>' + sum.pending + '</b><span>\u5f85\u8ba2\u6b63</span></div>' +
        '<div class="stat"><b>' + sum.missTotal + '</b><span>\u672a\u5b8c\u6210</span></div></div>' +
        '<div class="section-title">\u8fd1 ' + sum.bars.length + ' \u6b21\u4f5c\u4e1a\u5b8c\u6210\u7387</div>' +
        (sum.bars.length
          ? '<div class="chart">' + sum.bars.map(function (b) {
              return '<div class="bar" data-act="open-assignment" data-id="' + b.id + '"><em>' + b.rate + '%</em>' +
                '<i style="height:' + Math.max(6, Math.round(b.rate * 0.9)) + 'px"></i>' +
                '<span>' + esc(b.date.slice(5).replace('-', '/')) + '</span></div>';
            }).join('') + '</div>'
          : '<div class="banner"><span data-icon="info"></span>\u8fd8\u6ca1\u6709\u4f5c\u4e1a\u8bb0\u5f55</div>') +
        '<div class="section-title">\u672a\u5b8c\u6210\u5b66\u751f</div>' +
        (missRows.length ? missRows.join('') : '<div class="banner ok"><span data-icon="check"></span>\u6700\u8fd1\u4e24\u6b21\u4f5c\u4e1a\u5168\u90e8\u5b8c\u6210</div>') +
        '<div class="btn-row"><button class="btn outlined" data-act="export-csv">\u5bfc\u51fa CSV</button>' +
        '<button class="btn filled" data-act="export-excel">\u5bfc\u51fa Excel</button></div>';
    } else if (ui.reportTab === 'assignment') {
      var cur = currentAssignment();
      if (!cur) {
        body = '<div class="banner"><span data-icon="info"></span>\u8fd8\u6ca1\u6709\u4f5c\u4e1a\u8bb0\u5f55</div>';
      } else {
        var st2 = statsOf(cur.id, c.id);
        var rows = studentsOf(c.id).map(function (s) {
          var r = recordOf(cur.id, s.id);
          var cp = find(COMPLETION, r.completion);
          var cr = find(CORRECTION, r.correction);
          var g = find(GRADES, r.grade);
          return '<div class="data-row">' +
            '<span class="idx">' + esc(noText(c, s)) + '</span>' +
            '<span class="nm" title="' + esc(s.name) + '">' + esc(s.name) + '</span>' +
            '<span class="st" style="color:var(--st-' + cp.cls + ')">' + cp.text + '</span>' +
            '<span class="st" style="color:var(--st-' + cr.cls + ')">' + cr.text + '</span>' +
            '<span class="gd">' + g.label + '</span></div>';
        }).join('');
        body = rowField('\u4f5c\u4e1a', selectHtml(cur.title + ' \u00b7 ' + cur.date.slice(5).replace('-', '/'), 'pick-assignment')) +
          '<div class="stat-row compact">' +
          '<div class="stat"><b>' + st2.rate + '%</b><span>\u5b8c\u6210\u7387</span></div>' +
          '<div class="stat"><b>' + st2.done + '</b><span>\u5df2\u5b8c\u6210</span></div>' +
          '<div class="stat"><b>' + st2.partial + '</b><span>\u90e8\u5206\u5b8c\u6210</span></div>' +
          '<div class="stat"><b>' + st2.pending + '</b><span>\u5f85\u8ba2\u6b63</span></div></div>' +
          '<div class="section-title">\u5b8c\u6210\u60c5\u51b5\u660e\u7ec6</div>' +
          '<div class="t-assignment">' +
          '<div class="data-row head">' +
          '<span class="idx">\u5b66\u53f7</span><span class="nm">\u59d3\u540d</span>' +
          '<span class="st">\u5b8c\u6210\u60c5\u51b5</span>' +
          '<span class="st">\u8ba2\u6b63\u60c5\u51b5</span>' +
          '<span class="gd">\u8bc4\u7ea7</span></div>' + rows + '</div>' +
          '<div class="btn-row"><button class="btn outlined" data-act="export-one-csv">\u5bfc\u51fa CSV</button>' +
          '<button class="btn filled" data-act="export-one-excel">\u5bfc\u51fa Excel</button></div>';
      }
    } else {
      var list = studentsOf(c.id);
      var sid = ui.personId || (list[0] && list[0].id);
      var stu = null;
      list.forEach(function (s) { if (s.id === sid) stu = s; });
      var items = assignmentsOf(c.id).map(function (a) {
        var r = recordOf(a.id, stu.id);
        var comp = find(COMPLETION, r.completion);
        var corr = find(CORRECTION, r.correction);
        var g = find(GRADES, r.grade);
        return '<div class="data-row">' +
          '<span class="nm" title="' + esc(a.title) + '">' + esc(a.title) + '</span>' +
          '<span class="dt">' + esc(a.date.slice(5).replace('-', '/')) + '</span>' +
          '<span class="st" style="color:var(--st-' + comp.cls + ')">' + comp.text + '</span>' +
          '<span class="st" style="color:var(--st-' + corr.cls + ')">' + corr.text + '</span>' +
          '<span class="gd">' + g.label + '</span></div>';
      }).join('');
      var all = assignmentsOf(c.id);
      var okCount = 0;
      all.forEach(function (a) { if (COUNTED[recordOf(a.id, stu.id).completion]) okCount++; });
      var rate = all.length ? Math.round(okCount * 100 / all.length) : 0;
      body = rowField('\u5b66\u751f', selectHtml(noText(c, stu) + ' ' + stu.name, 'pick-person')) +
        '<div class="stat-row">' +
        '<div class="stat"><b>' + rate + '%</b><span>\u4e2a\u4eba\u5b8c\u6210\u7387</span></div>' +
        '<div class="stat"><b>' + okCount + '</b><span>\u5df2\u5b8c\u6210</span></div>' +
        '<div class="stat"><b>' + (all.length - okCount) + '</b><span>\u672a\u5b8c\u6210</span></div></div>' +
        '<div class="section-title">\u9010\u6b21\u660e\u7ec6</div>' +
        '<div class="t-person">' +
        '<div class="data-row head">' +
        '<span class="nm">\u4f5c\u4e1a</span>' +
        '<span class="dt">\u65e5\u671f</span>' +
        '<span class="st">\u5b8c\u6210\u60c5\u51b5</span>' +
        '<span class="st">\u8ba2\u6b63\u60c5\u51b5</span>' +
        '<span class="gd">\u8bc4\u7ea7</span></div>' + items + '</div>';
    }
    return '<div class="top-bar"><div class="top-bar-title" style="padding-left:16px">\u7edf\u8ba1\u62a5\u8868</div>' +
      classSelectHtml() + '</div>' +
      '<div class="tab-row"><div class="tab' + (ui.reportTab === 'class' ? ' active' : '') + '" data-act="report-tab" data-v="class">\u73ed\u7ea7\u62a5\u8868</div>' +
      '<div class="tab' + (ui.reportTab === 'assignment' ? ' active' : '') + '" data-act="report-tab" data-v="assignment">\u5355\u6b21\u4f5c\u4e1a</div>' +
      '<div class="tab' + (ui.reportTab === 'person' ? ' active' : '') + '" data-act="report-tab" data-v="person">\u4e2a\u4eba\u62a5\u8868</div></div>' +
      '<div class="body">' + body + '</div>' + navHtml('report');
  }
};

/* ---------- 屏：二维码打印 ---------- */

SCREENS.qr = {
  label: function () { return '\u8bbe\u7f6e \u00b7 \u4e8c\u7ef4\u7801\u6253\u5370 \u00b7 \u9644\u5f55 A'; },
  html: function () {
    var c = currentClass();
    var list = c ? studentsOf(c.id) : [];
    var per = store.settings.qrPerRow * 4;
    var page = list.slice(0, per);
    var qs = Math.max(22, 50 - (store.settings.qrPerRow - 2) * 4);
    var sheet = page.length
      ? '<div class="sheet"><div class="sticker-grid" style="--qr-size:' + qs + 'px;grid-template-columns:repeat(' + store.settings.qrPerRow + ',1fr)">' +
        page.map(function (s) {
          return '<div class="sticker"><div class="qr"></div><b>' + esc(s.name) + '</b>' +
            (store.settings.qrWithNo ? '<span>' + esc(noText(c, s)) + '</span>' : '') + '</div>';
        }).join('') + '</div></div>' +
        '<div class="sheet-caption">A4 \u6392\u7248\u9884\u89c8 \u00b7 ' + store.settings.qrPerRow + ' \u00d7 4 = ' + per +
        ' \u679a / \u9875 \u00b7 \u5171 ' + list.length + ' \u4eba \u00b7 \u865a\u7ebf\u4e3a\u88c1\u5207\u8fb9</div>'
      : emptyHtml('qr', '\u6ca1\u6709\u5b66\u751f', '\u5148\u5bfc\u5165\u540d\u5355\u624d\u80fd\u751f\u6210\u8d34\u7eb8');
    return '<div class="top-bar">' + backBtn() +
      '<div class="top-bar-title">\u4e8c\u7ef4\u7801\u6253\u5370</div>' +
      '<button class="icon-btn" data-act="print" data-icon="print" aria-label="\u6253\u5370"></button></div>' +
      '<div class="body">' +
      rowField('\u73ed\u7ea7', c ? selectHtml(c.name, 'pick-class') : selectHtml('\u65e0', 'noop')) +
      sheet +
      '<div class="field-hint" style="padding:0 16px 12px">\u6392\u7248\u53c2\u6570\uff08\u6bcf\u884c\u4e2a\u6570 / \u7eb8\u5f20\u5927\u5c0f / \u5bb9\u9519\u7ea7\u522b / \u9875\u8fb9\u8ddd / \u5305\u542b\u5b66\u53f7\uff09\u5df2\u7edf\u4e00\u5230\u300c\u8bbe\u7f6e \u00b7 \u4e8c\u7ef4\u7801\u9ed8\u8ba4\u53c2\u6570\u300d\u3002</div>' +
      '<div class="btn-row"><button class="btn outlined" data-act="print">\u9884\u89c8</button>' +
      '<button class="btn filled" data-act="print">\u6253\u5370</button></div></div>';
  }
};

/* ---------- 屏：设置 ---------- */

SCREENS.settings = {
  label: function () { return 'Tab 4 \u00b7 \u8bbe\u7f6e \u00b7 FR-9'; },
  html: function () {
    var st = store.settings;
    var dc = find(COMPLETION, st.defaultCompletion);
    var dr = find(CORRECTION, st.defaultCorrection);
    var wd = st.webdav;
    return '<div class="top-bar"><div class="top-bar-title" style="padding-left:16px">\u8bbe\u7f6e</div></div>' +
      '<div class="body">' +
      '<div class="group-title">\u8bb0\u5f55\u9ed8\u8ba4\u503c</div>' +
      rowField('\u9ed8\u8ba4\u5b8c\u6210\u72b6\u6001', selectHtml(dc.text, 'pick-defcomp')) +
      rowField('\u9ed8\u8ba4\u8ba2\u6b63\u72b6\u6001', selectHtml(dr.text, 'pick-defcorr')) +
      '<div class="field-hint" style="padding:0 16px 12px">\u4fee\u6539\u540e\u7acb\u5373\u4f5c\u7528\u4e8e\u6240\u6709\u5c1a\u65e0\u8bb0\u5f55\u7684\u5b66\u751f\uff0c\u5df2\u624b\u52a8\u6539\u8fc7\u7684\u8bb0\u5f55\u4e0d\u53d7\u5f71\u54cd\u3002\u8bc4\u7ea7\u6c38\u8fdc\u9ed8\u8ba4\u300c\u672a\u8bc4\u300d\u3002</div>' +
      '<div class="group-divider"></div>' +
      '<div class="group-title">\u4e8c\u7ef4\u7801\u9ed8\u8ba4\u53c2\u6570</div>' +
      rowField('\u6bcf\u884c\u4e2a\u6570', selectHtml(String(st.qrPerRow), 'pick-perrow')) +
      rowField('\u7eb8\u5f20\u5927\u5c0f', selectHtml('A4', 'noop')) +
      rowField('\u5bb9\u9519\u7ea7\u522b', selectHtml(st.qrEcc, 'pick-ecc')) +
      rowField('\u9875\u8fb9\u8ddd', selectHtml(st.qrMargin + ' mm', 'pick-margin')) +
      rowField('\u9ed8\u8ba4\u5305\u542b\u5b66\u53f7', switchHtml(st.qrWithNo, 'toggle-qrno')) +
      navRow('qr', '\u4e8c\u7ef4\u7801\u6253\u5370', '\u9884\u89c8\u4e0e\u6253\u5370\u5f53\u524d\u73ed\u7ea7\u8d34\u7eb8', 'open-qr') +
      '<div class="group-divider"></div>' +
      '<div class="group-title">\u4e91\u540c\u6b65</div>' +
      navRow('cloud', 'WebDAV \u914d\u7f6e',
        wd.enabled ? (wd.url ? '\u5df2\u542f\u7528 \u00b7 ' + wd.url : '\u5df2\u542f\u7528 \u00b7 \u5c1a\u672a\u586b\u5199\u5730\u5740') : '\u672a\u542f\u7528',
        'open-webdav') +
      navRow('sync', '\u81ea\u52a8\u5907\u4efd',
        st.autoBackup ? '\u5f00 \u00b7 \u4e0a\u6b21\u4e0a\u4f20 ' + (st.lastBackupAt || '\u2014') : '\u5173',
        'open-backup') +
      '<div class="group-divider"></div>' +
      '<div class="group-title">\u6570\u636e\u7ba1\u7406</div>' +
      navRow('cloudUp', '\u5bfc\u51fa\u5907\u4efd\u6587\u4ef6', 'hwt-backup \u00b7 JSON \u00b7 \u4e0d\u542b WebDAV \u5bc6\u7801', 'export-backup') +
      navRow('cloudDown', '\u4ece\u6587\u4ef6\u6062\u590d', '\u5c06\u8986\u76d6\u5f53\u524d\u672c\u5730\u6570\u636e', 'import-backup') +
      navRow('trash', '\u6e05\u7a7a\u5168\u90e8\u6570\u636e', '\u4e0d\u53ef\u64a4\u9500\uff0c\u8bf7\u5148\u5bfc\u51fa\u5907\u4efd', 'wipe', ' data-danger="1"') +
      '<div class="group-divider"></div>' +
      '<div class="group-title">\u5173\u4e8e</div>' +
      rowField('\u7248\u672c', '<span class="label" style="color:var(--md-on-surface)">\u539f\u578b v0.3</span>') +
      rowField('\u6743\u9650', '<span class="label" style="color:var(--md-on-surface)">\u4ec5\u76f8\u673a</span>') +
      '</div>' + navHtml('settings');
  }
};

/* ---------- 屏：WebDAV 配置 ---------- */

SCREENS.webdav = {
  label: function () { return 'WebDAV \u914d\u7f6e \u00b7 FR-9.3 / FR-10.3'; },
  html: function () {
    var wd = store.settings.webdav;
    var on = wd.enabled;
    var banner = '';
    if (wd.tested === 'ok') {
      banner = '<div class="banner ok"><span data-icon="check"></span><span>\u8fde\u63a5\u6b63\u5e38\uff0c\u76ee\u5f55\u53ef\u5199\u5165\u3002</span></div>';
    } else if (wd.tested === 'fail') {
      banner = '<div class="banner err"><span data-icon="info"></span><span>\u8fde\u63a5\u5931\u8d25\uff1a\u8bf7\u68c0\u67e5\u5730\u5740\u3001\u7528\u6237\u540d\u6216\u5bc6\u7801\u3002</span></div>';
    }
    var form = !on ? '<div class="field-hint" style="padding:16px">\u542f\u7528\u540e\u53ef\u586b\u5199\u670d\u52a1\u5668\u4fe1\u606f\u3002\u5173\u95ed\u65f6\u4e0d\u4f1a\u4e0a\u4f20\u4efb\u4f55\u6570\u636e\u3002</div>' :
      '<div class="field"><div class="label">\u76ee\u5f55\u5730\u5740</div>' +
      '<div class="input"><input type="text" data-field="wd-url" value="' + esc(wd.url) + '" placeholder="https://dav.example.com/hwt/"></div>' +
      '<div class="field-hint">\u9700\u4ee5 / \u7ed3\u5c3e\uff0c\u5907\u4efd\u6587\u4ef6\u76f4\u63a5\u653e\u5728\u8be5\u76ee\u5f55\u4e0b\u3002</div></div>' +
      '<div class="field"><div class="label">\u7528\u6237\u540d</div>' +
      '<div class="input"><input type="text" data-field="wd-user" value="' + esc(wd.user) + '" placeholder="\u8d26\u53f7"></div></div>' +
      '<div class="field"><div class="label">\u5bc6\u7801</div>' +
      '<div class="input"><input type="' + (ui.pwVisible ? 'text' : 'password') + '" data-field="wd-pass" value="' + esc(wd.password) + '" placeholder="\u5e94\u7528\u5bc6\u7801">' +
      '<span class="grow"></span><span data-act="toggle-pw" data-icon="eye" style="cursor:pointer;color:var(--md-on-surface-variant)"></span></div></div>' +
      '<div class="banner"><span data-icon="lock"></span><span>\u5bc6\u7801\u7ecf Android KeyStore \u52a0\u5bc6\u540e\u5b58\u5165 EncryptedSharedPreferences\uff0c\u4e0d\u4f1a\u5199\u5165\u5907\u4efd\u6587\u4ef6\u3002</span></div>' +
      rowField('\u4e91\u7aef\u4fdd\u7559\u4efd\u6570', selectHtml(wd.keep + ' \u4efd', 'pick-keep')) +
      '<div class="field-hint" style="padding:0 16px 12px">\u8d85\u8fc7\u4efd\u6570\u65f6\u81ea\u52a8\u5220\u9664\u6700\u65e7\u7684\u5907\u4efd\u3002</div>' +
      banner +
      '<div class="btn-row"><button class="btn outlined" data-act="wd-test">\u6d4b\u8bd5\u8fde\u63a5</button>' +
      '<button class="btn filled" data-act="wd-save">\u4fdd\u5b58</button></div>';
    return '<div class="top-bar">' + backBtn() +
      '<div class="top-bar-title">WebDAV \u914d\u7f6e</div></div>' +
      '<div class="body">' +
      rowField('\u542f\u7528 WebDAV \u540c\u6b65', switchHtml(on, 'toggle-wd')) +
      form + '</div>';
  }
};

/* ---------- 屏：备份管理 ---------- */

SCREENS.backup = {
  label: function () { return '\u5907\u4efd\u7ba1\u7406 \u00b7 FR-10 / \u9644\u5f55 D'; },
  html: function () {
    var st = store.settings;
    var wd = st.webdav;
    var cloud = wd.enabled
      ? (store.cloud.length
        ? store.cloud.map(function (f, i) {
          return '<div class="cloud-item' + (f.latest ? ' latest' : '') + '">' +
            '<span class="ci-icon" data-icon="file"></span>' +
            '<div class="ci-text"><div class="ci-title">' + esc(f.name) + '</div><div class="ci-sub">' + esc(f.size + ' \u00b7 ' + f.meta) + (f.latest ? ' \u00b7 \u6700\u65b0' : '') + '</div></div>' +
            '<button class="btn text" data-act="cloud-restore" data-i="' + i + '">\u6062\u590d</button></div>';
        }).join('')
        : emptyHtml('cloud', '\u4e91\u7aef\u8fd8\u6ca1\u6709\u5907\u4efd', '\u4fee\u6539\u6570\u636e\u540e\u4f1a\u81ea\u52a8\u4e0a\u4f20\u4e00\u4efd'))
      : '<div class="field-hint" style="padding:0 16px 16px">\u5c1a\u672a\u542f\u7528 WebDAV\uff0c\u53ea\u80fd\u4f7f\u7528\u672c\u5730\u5bfc\u51fa / \u5bfc\u5165\u3002</div>';
    return '<div class="top-bar">' + backBtn() +
      '<div class="top-bar-title">\u5907\u4efd\u7ba1\u7406</div>' +
      '<button class="icon-btn" data-act="cloud-upload" data-icon="cloudUp" aria-label="\u7acb\u5373\u4e0a\u4f20"></button></div>' +
      '<div class="body">' +
      '<div class="group-title">\u81ea\u52a8\u5907\u4efd</div>' +
      rowField('\u53d8\u66f4\u540e\u81ea\u52a8\u4e0a\u4f20', switchHtml(st.autoBackup, 'toggle-auto')) +
      '<div class="field-hint" style="padding:0 16px 12px">\u6570\u636e\u53d8\u66f4\u540e\u5ef6\u8fdf 8 \u79d2\u5408\u5e76\u4e0a\u4f20\uff0c\u907f\u514d\u9891\u7e41\u5199\u4e91\u7aef\u3002</div>' +
      rowField('\u4e0a\u6b21\u4e0a\u4f20', '<span class="label" style="color:var(--md-on-surface)">' + esc(st.lastBackupAt || '\u5c1a\u672a\u4e0a\u4f20') + '</span>') +
      '<div class="group-divider"></div>' +
      '<div class="group-title">\u672c\u5730\u6587\u4ef6</div>' +
      navRow('cloudUp', '\u5bfc\u51fa\u5907\u4efd\u6587\u4ef6', '\u9009\u62e9\u4fdd\u5b58\u4f4d\u7f6e\u540e\u5199\u5165 JSON', 'export-backup') +
      navRow('cloudDown', '\u4ece\u6587\u4ef6\u6062\u590d', '\u6821\u9a8c format \u540e\u8986\u76d6\u672c\u5730\u6570\u636e', 'import-backup') +
      '<div class="group-divider"></div>' +
      '<div class="group-title">\u4e91\u7aef\u5907\u4efd</div>' + cloud +
      '<div class="banner"><span data-icon="info"></span><span>\u5907\u4efd\u4e3a hwt-backup \u683c\u5f0f\uff08version 1\uff09\uff0c\u5305\u542b\u8bbe\u7f6e\u3001\u73ed\u7ea7\u3001\u5b66\u751f\u3001\u4f5c\u4e1a\u4e0e\u8bb0\u5f55\uff0c\u4e0d\u5305\u542b WebDAV \u5bc6\u7801\u3002</span></div>' +
      '</div>';
  }
};

/* ---------- 屏：Excel 导入向导 ---------- */

var NAME_KEYS = ['\u5b66\u751f\u59d3\u540d', '\u59d3\u540d', '\u540d\u5b57', '\u5b66\u751f', 'name', 'student', 'studentname', 'fullname'];
var NOTE_KEYS = ['\u5907\u6ce8', '\u8bf4\u660e', '\u6ce8\u91ca', 'note', 'remark', 'comment'];

var DEMO_SHEET = {
  file: '\u4e09\u5e74\u7ea7\u4e8c\u73ed\u540d\u5355.xlsx',
  sheet: 'Sheet1',
  columns: [
    { name: '\u5e8f\u53f7', samples: ['1', '2', '3'] },
    { name: '\u5b66\u751f\u59d3\u540d', samples: ['\u4f55\u5a1f', '\u6768\u5e06', '\u6797\u971e'] },
    { name: '\u6027\u522b', samples: ['\u5973', '\u7537', '\u5973'] },
    { name: '\u5907\u6ce8', samples: ['\u4f4f\u6821', '', '\u82f1\u8bed\u5c0f\u7ec4'] }
  ],
  rows: [
    { name: '\u4f55\u5a1f', note: '\u4f4f\u6821' },
    { name: '\u6768\u5e06', note: '' },
    { name: '\u6797\u971e', note: '\u82f1\u8bed\u5c0f\u7ec4' },
    { name: '', note: '\u7a7a\u884c' },
    { name: '\u80e1\u5b87', note: '' },
    { name: '\u9a6c\u8d85', note: '' },
    { name: '\u6768\u5e06', note: '\u91cd\u590d' },
    { name: '\u7f57\u5a9b', note: '' },
    { name: '\u8c22\u5b87\u822a', note: '' },
    { name: '\u5085\u5c0f\u96e8', note: '\u8f6c\u5b66\u751f' }
  ]
};

function matchCol(colName, keys) {
  var k = String(colName).toLowerCase().replace(/\s/g, '');
  for (var i = 0; i < keys.length; i++) {
    if (k === keys[i].toLowerCase()) return true;
  }
  return false;
}

function autoMap() {
  var nameIdx = -1, noteIdx = -1;
  DEMO_SHEET.columns.forEach(function (c, i) {
    if (nameIdx < 0 && matchCol(c.name, NAME_KEYS)) nameIdx = i;
    if (noteIdx < 0 && matchCol(c.name, NOTE_KEYS)) noteIdx = i;
  });
  if (nameIdx < 0) nameIdx = 0;
  return { nameIdx: nameIdx, noteIdx: noteIdx };
}

function startWizard() {
  var m = autoMap();
  ui.wizard = { step: 1, nameIdx: m.nameIdx, noteIdx: m.noteIdx, picked: false };
  go('wizard');
}

function previewRows() {
  var c = currentClass();
  var exist = {};
  if (c) studentsOf(c.id).forEach(function (s) { exist[s.name] = 1; });
  var seen = {};
  return DEMO_SHEET.rows.map(function (r) {
    var name = String(r.name || '').replace(/^\s+|\s+$/g, '');
    var reason = '';
    if (!name) reason = 'empty';
    else if (seen[name]) reason = 'dup';
    else if (exist[name]) reason = 'exist';
    if (!reason) seen[name] = 1;
    return { name: name, note: ui.wizard && ui.wizard.noteIdx >= 0 ? r.note : '', reason: reason };
  });
}

function stepperHtml(step) {
  var names = ['\u9009\u6587\u4ef6', '\u8bc6\u522b\u5217', '\u9884\u89c8\u786e\u8ba4'];
  return '<div class="stepper">' + names.map(function (n, i) {
    var idx = i + 1;
    var cls = idx === step ? ' active' : (idx < step ? ' done' : '');
    return (i ? '<span class="step-line"></span>' : '') +
      '<div class="step' + cls + '"><i>' + (idx < step ? '\u2713' : idx) + '</i><span>' + n + '</span></div>';
  }).join('') + '</div>';
}

SCREENS.wizard = {
  label: function () {
    var s = ui.wizard ? ui.wizard.step : 1;
    return 'Excel \u5bfc\u5165 \u00b7 \u6b65\u9aa4 ' + s + '/3 \u00b7 FR-3.6';
  },
  html: function () {
    if (!ui.wizard) startWizardInline();
    var w = ui.wizard;
    var c = currentClass();
    var body = '';
    if (w.step === 1) {
      body = '<div class="section-title">\u9009\u62e9\u540d\u5355\u6587\u4ef6</div>' +
        navRow('folder', w.picked ? esc(DEMO_SHEET.file) : '\u4ece\u6587\u4ef6\u9009\u62e9\u5668\u9009\u53d6',
          w.picked ? DEMO_SHEET.sheet + ' \u00b7 ' + DEMO_SHEET.rows.length + ' \u884c \u00b7 ' + DEMO_SHEET.columns.length + ' \u5217'
            : '\u652f\u6301 .xlsx / .xls\uff0c\u53ea\u8bfb\u53d6\u7b2c\u4e00\u4e2a\u5de5\u4f5c\u8868', 'wz-pick') +
        '<div class="banner"><span data-icon="info"></span><span>\u540d\u5355\u53ea\u9700\u4e00\u5217\u59d3\u540d\u5373\u53ef\u3002\u8868\u5934\u5199\u300c\u59d3\u540d\u300d\u548c\u300c\u5907\u6ce8\u300d\u65f6\u80fd\u81ea\u52a8\u8bc6\u522b\u3002</span></div>' +
        (c ? '<div class="field-hint" style="padding:0 16px">\u5bfc\u5165\u5230\uff1a' + esc(c.name) + '</div>' : '') +
        '<div class="btn-row"><button class="btn filled tall" data-act="wz-next"' + (w.picked ? '' : ' disabled') + '>\u4e0b\u4e00\u6b65</button></div>';
    } else if (w.step === 2) {
      body = '<div class="section-title">\u786e\u8ba4\u5217\u5bf9\u5e94\u5173\u7cfb</div>' +
        '<div class="map-table">' + DEMO_SHEET.columns.map(function (col, i) {
          var tag = i === w.nameIdx ? '<span class="badge hit">\u59d3\u540d</span>'
            : (i === w.noteIdx ? '<span class="badge hit">\u5907\u6ce8</span>' : '<span class="badge">\u5ffd\u7565</span>');
          return '<div class="map-row" data-act="wz-col" data-i="' + i + '">' +
            '<span class="col-name">' + esc(col.name) + '</span>' +
            '<span class="col-sample">' + esc(col.samples.join('\u3001')) + '</span>' + tag + '</div>';
        }).join('') + '</div>' +
        '<div class="field-hint" style="padding:0 16px">\u70b9\u4efb\u610f\u4e00\u884c\u53ef\u91cd\u65b0\u6307\u5b9a\u8be5\u5217\u7684\u7528\u9014\u3002\u672a\u8bc6\u522b\u5230\u59d3\u540d\u5217\u65f6\u9ed8\u8ba4\u53d6\u7b2c\u4e00\u5217\u3002</div>' +
        rowField('\u5b66\u53f7\u7f16\u53f7', '<span class="label" style="color:var(--md-on-surface)">\u6309\u5f53\u524d\u73ed\u89c4\u5219\u7eed\u53f7</span>') +
        '<div class="btn-row"><button class="btn outlined" data-act="wz-prev">\u4e0a\u4e00\u6b65</button>' +
        '<button class="btn filled" data-act="wz-next">\u9884\u89c8</button></div>';
    } else {
      var rows = previewRows();
      var ok = rows.filter(function (r) { return !r.reason; });
      var skipped = rows.length - ok.length;
      body = '<div class="stat-row">' +
        '<div class="stat"><b>' + rows.length + '</b><span>\u8bfb\u5230\u884c\u6570</span></div>' +
        '<div class="stat"><b>' + ok.length + '</b><span>\u5c06\u5bfc\u5165</span></div>' +
        '<div class="stat"><b>' + skipped + '</b><span>\u5df2\u8df3\u8fc7</span></div></div>' +
        '<div class="section-title">\u9010\u884c\u9884\u89c8</div>' +
        rows.map(function (r, i) {
          var tag = r.reason === 'empty' ? '<span class="badge">\u7a7a\u884c</span>'
            : r.reason === 'dup' ? '<span class="badge dup">\u91cd\u590d</span>'
              : r.reason === 'exist' ? '<span class="badge dup">\u5df2\u5b58\u5728</span>'
                : '<span class="badge hit">\u2713</span>';
          return '<div class="preview-row' + (r.reason ? ' skip' : '') + '">' +
            '<span class="idx">' + (i + 1) + '</span>' +
            '<span class="nm">' + (r.name ? esc(r.name) : '\u2014') + '</span>' +
            (r.note ? '<span class="col-sample">' + esc(r.note) + '</span>' : '') + tag + '</div>';
        }).join('') +
        (skipped ? '<div class="banner err"><span data-icon="info"></span><span>\u7a7a\u884c\u4e0e\u540c\u540d\u884c\u4f1a\u81ea\u52a8\u8df3\u8fc7\uff0c\u4e0d\u4f1a\u8986\u76d6\u5df2\u6709\u5b66\u751f\u3002</span></div>' : '') +
        '<div class="btn-row"><button class="btn outlined" data-act="wz-prev">\u4e0a\u4e00\u6b65</button>' +
        '<button class="btn filled" data-act="wz-commit">\u5bfc\u5165 ' + ok.length + ' \u4eba</button></div>';
    }
    return '<div class="top-bar">' + backBtn() +
      '<div class="top-bar-title">\u5bfc\u5165\u5b66\u751f\u540d\u5355</div></div>' +
      stepperHtml(w.step) + '<div class="body">' + body + '</div>';
  }
};

function startWizardInline() {
  var m = autoMap();
  ui.wizard = { step: 1, nameIdx: m.nameIdx, noteIdx: m.noteIdx, picked: false };
}

function commitWizard() {
  var c = currentClass();
  if (!c) { toast('\u8bf7\u5148\u9009\u62e9\u73ed\u7ea7'); return; }
  var rows = previewRows().filter(function (r) { return !r.reason; });
  rows.forEach(function (r) {
    store.students.push({
      id: uid('s'), classId: c.id, no: nextNo(c), name: r.name, note: r.note || ''
    });
  });
  save();
  ui.wizard = null;
  ui.classTab = 'students';
  back();
  toast('\u5df2\u5bfc\u5165 ' + rows.length + ' \u4eba');
}

/* ---------- 事件层：选择器 ---------- */

function optList(list) {
  return list.map(function (o) { return { v: o.v, label: o.text }; });
}

function numOpts(arr, suffix) {
  return arr.map(function (n) {
    return { v: String(n), label: String(n) + (suffix || '') };
  });
}

var PICKERS = {
  'pick-class': function () {
    return { title: '\u5207\u6362\u73ed\u7ea7', value: store.currentClassId,
      options: store.classes.map(function (c) { return { v: c.id, label: c.name }; }) };
  },
  'pick-person': function () {
    var list = studentsOf(store.currentClassId);
    return { title: '\u9009\u62e9\u5b66\u751f', value: ui.personId || (list[0] && list[0].id),
      options: list.map(function (s) { return { v: s.id, label: s.name }; }) };
  },
  'pick-assignment': function () {
    var list = assignmentsOf(store.currentClassId);
    var cur = currentAssignment();
    return { title: '\u9009\u62e9\u4f5c\u4e1a', value: cur && cur.id,
      options: list.map(function (a) {
        return { v: a.id, label: a.title + ' \u00b7 ' + a.date.slice(5).replace('-', '/') };
      }) };
  },
  'pick-defcomp': function () {
    return { title: '\u9ed8\u8ba4\u5b8c\u6210\u72b6\u6001', value: store.settings.defaultCompletion, options: optList(COMPLETION) };
  },
  'pick-defcorr': function () {
    return { title: '\u9ed8\u8ba4\u8ba2\u6b63\u72b6\u6001', value: store.settings.defaultCorrection, options: optList(CORRECTION) };
  },
  'pick-perrow': function () {
    return { title: '\u6bcf\u884c\u4e2a\u6570', value: String(store.settings.qrPerRow), options: numOpts([2, 3, 4, 5, 6, 7, 8]) };
  },
  'pick-ecc': function () {
    return { title: '\u5bb9\u9519\u7ea7\u522b', value: store.settings.qrEcc,
      options: ['L', 'M', 'Q', 'H'].map(function (x) { return { v: x, label: x }; }) };
  },
  'pick-margin': function () {
    return { title: '\u9875\u8fb9\u8ddd', value: String(store.settings.qrMargin), options: numOpts([0, 1, 2, 4], ' mm') };
  },
  'pick-keep': function () {
    return { title: '\u4e91\u7aef\u4fdd\u7559\u4efd\u6570', value: String(store.settings.webdav.keep), options: numOpts([3, 5, 10], ' \u4efd') };
  },
  'wz-col': function () {
    var w = ui.wizard, cur = 'ignore';
    if (w.colIdx === w.nameIdx) cur = 'name';
    else if (w.colIdx === w.noteIdx) cur = 'note';
    return { title: '\u8be5\u5217\u7528\u9014', value: cur, options: [
      { v: 'name', label: '\u59d3\u540d\u5217' },
      { v: 'note', label: '\u5907\u6ce8\u5217' },
      { v: 'ignore', label: '\u5ffd\u7565' }
    ] };
  }
};

function applyPick(act, v) {
  var st = store.settings;
  if (act === 'pick-class') { store.currentClassId = v; ui.personId = null; ui.reportAid = null; }
  else if (act === 'pick-person') { ui.personId = v; }
  else if (act === 'pick-assignment') { ui.reportAid = v; }
  else if (act === 'pick-defcomp') { st.defaultCompletion = v; }
  else if (act === 'pick-defcorr') { st.defaultCorrection = v; }
  else if (act === 'pick-perrow') { st.qrPerRow = parseInt(v, 10); }
  else if (act === 'pick-ecc') { st.qrEcc = v; }
  else if (act === 'pick-margin') { st.qrMargin = parseInt(v, 10); }
  else if (act === 'pick-keep') { st.webdav.keep = parseInt(v, 10); }
  else if (act === 'wz-col') {
    var w = ui.wizard, i = w.colIdx;
    if (v === 'name') { if (w.noteIdx === i) w.noteIdx = -1; w.nameIdx = i; }
    else if (v === 'note') { if (w.nameIdx === i) w.nameIdx = -1; w.noteIdx = i; }
    else { if (w.nameIdx === i) w.nameIdx = -1; if (w.noteIdx === i) w.noteIdx = -1; }
  }
  save();
}

/* ---------- 事件层：动作实现 ---------- */

function askText(title, value, placeholder, hint, onOk) {
  openDialog({ title: title, input: value || '', placeholder: placeholder || '',
    hint: hint || '', confirm: '\u786e\u5b9a', act: 'text-ok', onOk: onOk });
}

function addAssignment() {
  var c = currentClass();
  if (!c) { toast('\u8bf7\u5148\u521b\u5efa\u73ed\u7ea7'); return; }
  var n = assignmentsOf(c.id).length;
  askText('\u5e03\u7f6e\u4f5c\u4e1a', DEMO_TITLES[n % DEMO_TITLES.length],
    '\u4f5c\u4e1a\u540d\u79f0', '\u65e5\u671f\u9ed8\u8ba4\u4e3a\u4eca\u5929\uff0c\u65b0\u4f5c\u4e1a\u4e0d\u4f1a\u9884\u5148\u751f\u6210\u8bb0\u5f55\u3002',
    function (text) {
      if (!text) { toast('\u8bf7\u8f93\u5165\u4f5c\u4e1a\u540d\u79f0'); return false; }
      var d = new Date();
      var date = d.getFullYear() + '-' + pad(d.getMonth() + 1, 2) + '-' + pad(d.getDate(), 2);
      var a = { id: uid('a'), classId: c.id, title: text, date: date };
      store.assignments.push(a);
      save();
      go('entry', { id: a.id });
      toast('\u5df2\u5e03\u7f6e\uff1a' + text);
      return true;
    });
}

function addStudent() {
  var c = currentClass();
  if (!c) { toast('\u8bf7\u5148\u521b\u5efa\u73ed\u7ea7'); return; }
  askText('\u6dfb\u52a0\u5b66\u751f', '', '\u5b66\u751f\u59d3\u540d',
    '\u5b66\u53f7\u5c06\u81ea\u52a8\u7f16\u4e3a ' + noText(c, { no: nextNo(c) }),
    function (text) {
      if (!text) { toast('\u8bf7\u8f93\u5165\u59d3\u540d'); return false; }
      store.students.push({ id: uid('s'), classId: c.id, no: nextNo(c), name: text, note: '' });
      save();
      ui.classTab = 'students';
      toast('\u5df2\u6dfb\u52a0\uff1a' + text);
      return true;
    });
}

function clsSampleHtml() {
  return '<div class="field" style="padding-top:0"><label>\u5b66\u53f7\u524d\u7f00\uff08\u53ef\u7a7a\uff09</label>' +
    '<div class="input"><input data-field="cls-prefix" placeholder="\u5982\uff1a43"></div></div>' +
    '<div class="field"><label>\u8865\u96f6\u4f4d\u6570</label>' +
    '<div class="input"><input data-field="cls-digits" type="number" min="1" max="4" value="2"></div>' +
    '<div class="field-hint" data-role="cls-sample">\u793a\u4f8b\u5b66\u53f7\uff1a01\u300102\u300103\u2026</div></div>' +
    rowField('\u56de\u6536\u7a7a\u53f7', switchHtml(ui.clsRule.recycle, 'cls-recycle'));
}

function refreshClsSample() {
  var hint = document.querySelector('[data-role="cls-sample"]');
  if (!hint) return;
  var pre = document.querySelector('[data-field="cls-prefix"]');
  var dig = document.querySelector('[data-field="cls-digits"]');
  var d = parseInt(dig && dig.value, 10);
  if (!(d >= 1 && d <= 4)) d = 2;
  hint.textContent = '\u793a\u4f8b\u5b66\u53f7\uff1a' + [1, 2, 3].map(function (n) {
    return (pre ? String(pre.value).trim() : '') + pad(n, d);
  }).join('\u3001') + '\u2026';
}

function addClass() {
  ui.clsRule.recycle = false;
  openDialog({
    title: '\u65b0\u5efa\u73ed\u7ea7',
    form: '<div class="field" style="padding-top:0"><label>\u73ed\u7ea7\u540d\u79f0</label>' +
      '<div class="input"><input data-field="cls-name" placeholder="\u4f8b\uff1a\u4e94\u73ed\uff083\uff09"></div></div>' +
      clsSampleHtml(),
    confirm: '\u521b\u5efa', act: 'cls-create', cancel: '\u53d6\u6d88'
  });
}

function editClassNoRule(cid) {
  var c = classById(cid);
  if (!c) return;
  ui.clsRule.recycle = !!c.recycle;
  openDialog({
    title: '\u5b66\u53f7\u89c4\u5219',
    form: '<div class="field" style="padding-top:0"><label>\u5b66\u53f7\u524d\u7f00\uff08\u53ef\u7a7a\uff09</label>' +
      '<div class="input"><input data-field="cls-prefix" value="' + esc(c.prefix || '') + '" placeholder="\u5982\uff1a43"></div></div>' +
      '<div class="field"><label>\u8865\u96f6\u4f4d\u6570</label>' +
      '<div class="input"><input data-field="cls-digits" type="number" min="1" max="4" value="' + (c.digits || 2) + '"></div>' +
      '<div class="field-hint" data-role="cls-sample">\u793a\u4f8b\u5b66\u53f7\uff1a' +
      esc(noText(c, { no: 1 })) + '\u3001' + esc(noText(c, { no: 2 })) + '\u3001' + esc(noText(c, { no: 3 })) + '\u2026</div></div>' +
      rowField('\u56de\u6536\u7a7a\u53f7', switchHtml(ui.clsRule.recycle, 'cls-recycle')),
    confirm: '\u4fdd\u5b58', act: 'cls-rule-save', id: cid, cancel: '\u53d6\u6d88'
  });
}

function createClassFromDraft() {
  var name = String(draft.name || '').trim();
  if (!name) { toast('\u8bf7\u5148\u586b\u5199\u73ed\u7ea7\u540d\u79f0'); return; }
  var digits = parseInt(draft.digits, 10);
  if (!(digits >= 1 && digits <= 4)) digits = 2;
  var c = { id: uid('c'), name: name, note: String(draft.note || '').trim(),
    prefix: String(draft.prefix || '').trim(), digits: digits, recycle: !!draft.recycle };
  store.classes.push(c);
  store.currentClassId = c.id;
  store.onboarded = true;
  save();
  draft.name = ''; draft.note = ''; draft.prefix = ''; draft.digits = 2; draft.recycle = false;
  ui.classTab = 'class';
  ui.expandedClassId = c.id;
  goTab('class');
  toast('\u73ed\u7ea7\u5df2\u521b\u5efa\uff0c\u4e0b\u4e00\u6b65\u5bfc\u5165\u540d\u5355');
}

function bulkApply(aid, cid) {
  var patch = pickPatch(ui.bulkPick);
  if (!patch) { toast('\u8bf7\u5148\u9009\u62e9\u8981\u6279\u91cf\u8bbe\u7f6e\u7684\u9879'); return; }
  var list = studentsOf(cid);
  list.forEach(function (s) { writeRecord(aid, s.id, patch); });
  toast('\u5df2\u6279\u91cf\u8bbe\u4e3a\uff1a' + pickSummary(ui.bulkPick) + '\uff08' + list.length + ' \u4eba\uff09');
}

function scanHit() {
  var patch = pickPatch(ui.scanPick);
  if (!patch) { toast('\u8bf7\u5148\u9009\u62e9\u8981\u6807\u8bb0\u7684\u72b6\u6001'); return; }
  var a = null;
  store.assignments.forEach(function (x) { if (x.id === (route.params && route.params.aid)) a = x; });
  if (!a) {
    var cid = store.currentClassId;
    a = assignmentsOf(cid)[0] || null;
  }
  if (!a) { toast('\u6ca1\u6709\u53ef\u5f55\u5165\u7684\u4f5c\u4e1a'); return; }
  var list = studentsOf(a.classId);
  var pool = list.filter(function (s) {
    var r = recordOf(a.id, s.id);
    var same = true;
    MARK_GROUPS.forEach(function (g) {
      if (patch[g.key] !== undefined && r[g.key] !== patch[g.key]) same = false;
    });
    return !(same && r.saved);
  });
  if (!pool.length) { toast('\u5168\u73ed\u90fd\u5df2\u662f\u8be5\u72b6\u6001'); return; }
  var s = pool[Math.floor(Math.random() * pool.length)];
  writeRecord(a.id, s.id, patch);
  ui.scanLog.push(s.name + ' \u00b7 ' + pickSummary(ui.scanPick));
}

function studentMenu(id) {
  var s = studentById(id);
  if (!s) return;
  var c = classById(s.classId);
  if (!c) return;
  openDialog({
    title: noText(c, s) + '\u3000' + s.name,
    menu: [
      { act: 'stu-name', icon: 'tabClass', label: '\u4fee\u6539\u59d3\u540d', sub: s.name },
      { act: 'stu-no', icon: 'qr', label: '\u4fee\u6539\u5b66\u53f7\u5e8f\u53f7',
        sub: '\u5f53\u524d\u5e8f\u53f7 ' + s.no + '\uff0c\u5b66\u53f7 ' + noText(c, s) },
      { act: 'stu-note', icon: 'info', label: '\u4fee\u6539\u5907\u6ce8',
        sub: s.note || '\u6682\u65e0\u5907\u6ce8' },
      { act: 'stu-del', icon: 'trash', label: '\u5220\u9664\u5b66\u751f',
        sub: '\u4f1a\u540c\u65f6\u5220\u9664\u5176\u5168\u90e8\u4f5c\u4e1a\u8bb0\u5f55', danger: true }
    ],
    id: id, cancel: '\u5173\u95ed'
  });
}

function studentMenuPick(act, id) {
  if (act === 'stu-name') renameStudent(id);
  else if (act === 'stu-no') editStudentNo(id);
  else if (act === 'stu-note') editStudentNote(id);
  else if (act === 'stu-del') confirmDelStudent(id);
}

function classEditMenu(cid) {
  var c = classById(cid);
  if (!c) return;
  var n = studentsOf(cid).length;
  var m = assignmentsOf(cid).length;
  openDialog({
    title: c.name + ' \u00b7 \u73ed\u7ea7\u8bbe\u7f6e',
    menuAct: 'class-edit',
    menu: [
      { act: 'cls-rename', icon: 'tabClass', label: '\u4fee\u6539\u73ed\u7ea7\u540d\u79f0', sub: c.name },
      { act: 'cls-note', icon: 'info', label: '\u4fee\u6539\u5907\u6ce8',
        sub: c.note || '\u6682\u65e0\u5907\u6ce8' },
      { act: 'cls-rule', icon: 'qr', label: '\u5b66\u53f7\u89c4\u5219',
        sub: '\u524d\u7f00 ' + (c.prefix || '\u65e0') + ' \u00b7 \u8865\u96f6 ' + (c.digits || 2) + ' \u4f4d \u00b7 \u56de\u6536\u7a7a\u53f7 ' + (c.recycle ? '\u5f00' : '\u5173') },
      { act: 'cls-renumber', icon: 'restore', label: '\u4e00\u952e\u91cd\u6392\u5b66\u53f7',
        sub: '\u6309\u5f53\u524d\u987a\u5e8f\u91cd\u65b0\u8fde\u7eed\u7f16\u53f7' },
      { act: 'cls-del', icon: 'trash', label: '\u5220\u9664\u73ed\u7ea7',
        sub: n + ' \u540d\u5b66\u751f \u00b7 ' + m + ' \u4efd\u4f5c\u4e1a\u5c06\u4e00\u5e76\u5220\u9664', danger: true }
    ],
    id: cid, cancel: '\u5173\u95ed'
  });
}

function classEditPick(act, cid) {
  if (act === 'cls-rename') renameClass(cid);
  else if (act === 'cls-note') editClassNote(cid);
  else if (act === 'cls-rule') editClassNoRule(cid);
  else if (act === 'cls-renumber') confirmRenumber(cid);
  else if (act === 'cls-del') confirmDelClass(cid);
}

function renameClass(cid) {
  var c = classById(cid);
  if (!c) return;
  askText('\u4fee\u6539\u73ed\u7ea7\u540d\u79f0', c.name, '\u73ed\u7ea7\u540d\u79f0', '',
    function (text) {
      if (!text) { toast('\u8bf7\u8f93\u5165\u73ed\u7ea7\u540d\u79f0'); return false; }
      c.name = text;
      save();
      toast('\u5df2\u6539\u4e3a\uff1a' + text);
      return true;
    });
}

function editClassNote(cid) {
  var c = classById(cid);
  if (!c) return;
  askText('\u4fee\u6539\u5907\u6ce8', c.note || '', '\u53ef\u7531\uff1a2025 \u79cb\u5b63 \u00b7 \u6570\u5b66', '',
    function (text) {
      c.note = text;
      save();
      toast(text ? '\u5907\u6ce8\u5df2\u4fdd\u5b58' : '\u5907\u6ce8\u5df2\u6e05\u7a7a');
      return true;
    });
}

function confirmDelClass(cid) {
  var c = classById(cid);
  if (!c) return;
  var n = studentsOf(cid).length;
  var m = assignmentsOf(cid).length;
  openDialog({ title: '\u5220\u9664' + c.name + '\uff1f',
    text: n + ' \u540d\u5b66\u751f\u3001' + m + ' \u4efd\u4f5c\u4e1a\u53ca\u5168\u90e8\u8bb0\u5f55\u5c06\u4e00\u5e76\u5220\u9664\uff0c\u4e0d\u53ef\u64a4\u9500\u3002',
    confirm: '\u5220\u9664', act: 'del-class', id: cid });
}

function delClass(cid) {
  var wasCurrent = store.currentClassId === cid;
  var aids = [];
  store.assignments.forEach(function (a) { if (a.classId === cid) aids.push(a.id); });
  store.assignments = store.assignments.filter(function (a) { return a.classId !== cid; });
  store.students = store.students.filter(function (s) { return s.classId !== cid; });
  Object.keys(store.records).forEach(function (k) {
    if (aids.indexOf(k.split('|')[0]) > -1) delete store.records[k];
  });
  store.classes = store.classes.filter(function (c) { return c.id !== cid; });
  if (wasCurrent) {
    store.currentClassId = store.classes.length ? store.classes[0].id : null;
    ui.personId = null;
    ui.reportAid = null;
  }
  if (ui.expandedClassId === cid) ui.expandedClassId = null;
  save();
}

function renumberGaps(list) {
  for (var i = 0; i < list.length; i++) {
    if (list[i].no !== i + 1) return true;
  }
  return false;
}

function confirmRenumber(cid) {
  var c = classById(cid);
  if (!c) return;
  var list = studentsOf(cid);
  if (!list.length) { toast('\u672c\u73ed\u8fd8\u6ca1\u6709\u5b66\u751f'); return; }
  if (!renumberGaps(list)) {
    toast('\u5b66\u53f7\u5df2\u662f\u8fde\u7eed\u7684 ' + noText(c, { no: 1 }) +
      '-' + noText(c, { no: list.length }) + '\uff0c\u65e0\u9700\u91cd\u6392');
    return;
  }
  openDialog({
    title: '\u91cd\u6392' + c.name + '\u5b66\u53f7\uff1f',
    text: '\u5c06\u6309\u5f53\u524d\u987a\u5e8f\u91cd\u7f16\u4e3a ' + noText(c, { no: 1 }) + '-' +
      noText(c, { no: list.length }) + '\uff08' + list.length + ' \u4eba\uff09\u3002\n' +
      '\u5df2\u6253\u5370\u8d34\u7eb8\u4e0a\u7684\u5b66\u53f7\u5c06\u4e0e\u7cfb\u7edf\u4e0d\u4e00\u81f4\uff0c\u5efa\u8bae\u91cd\u65b0\u6253\u5370\uff1b' +
      '\u4e8c\u7ef4\u7801\u6309\u5b66\u751f\u8eab\u4efd\u5b9a\u4f4d\uff0c\u626b\u7801\u5f55\u5165\u4ecd\u80fd\u5bf9\u5e94\u5230\u6b63\u786e\u7684\u4eba\u3002',
    confirm: '\u91cd\u6392', act: 'renumber-ok', id: cid
  });
}

function renumberClass(cid) {
  var c = classById(cid);
  var list = studentsOf(cid);
  var changed = 0;
  list.forEach(function (s, i) {
    if (s.no !== i + 1) { s.no = i + 1; changed++; }
  });
  save();
  toast('\u5df2\u91cd\u6392\u4e3a ' + noText(c, { no: 1 }) + '-' + noText(c, { no: list.length }) +
    '\uff08' + changed + ' \u4eba\u5b66\u53f7\u53d8\u66f4\uff09');
}

function renameStudent(id) {
  var s = studentById(id);
  if (!s) return;
  var c = classById(s.classId);
  askText('\u4fee\u6539\u59d3\u540d', s.name, '\u5b66\u751f\u59d3\u540d',
    '\u5b66\u53f7 ' + noText(c, s) + ' \u4fdd\u6301\u4e0d\u53d8',
    function (text) {
      if (!text) { toast('\u8bf7\u8f93\u5165\u59d3\u540d'); return false; }
      if (text === s.name) return true;
      s.name = text;
      save();
      toast('\u5df2\u6539\u4e3a\uff1a' + text);
      return true;
    });
}

function editStudentNote(id) {
  var s = studentById(id);
  if (!s) return;
  askText('\u4fee\u6539\u5907\u6ce8', s.note || '', '\u5982\uff1a\u7ec4\u957f\u3001\u9700\u91cd\u70b9\u5173\u6ce8',
    '\u7559\u7a7a\u5373\u6e05\u9664\u5907\u6ce8',
    function (text) {
      s.note = text;
      save();
      toast(text ? '\u5907\u6ce8\u5df2\u66f4\u65b0' : '\u5907\u6ce8\u5df2\u6e05\u9664');
      return true;
    });
}

function editStudentNo(id) {
  var s = studentById(id);
  if (!s) return;
  var c = classById(s.classId);
  askText('\u4fee\u6539\u5b66\u53f7\u5e8f\u53f7', String(s.no), '\u5e8f\u53f7\uff081-9999\uff09',
    '\u5f53\u524d\u5b66\u53f7 ' + noText(c, s) + '\uff08\u524d\u7f00 ' +
      (c.prefix || '\u65e0') + ' \u00b7 \u8865\u96f6 ' + (c.digits || 2) + ' \u4f4d\uff09\u3002' +
      '\u5df2\u6253\u5370\u7684\u4e8c\u7ef4\u7801\u4e0d\u53d7\u5f71\u54cd\u3002',
    function (text) {
      if (!/^\d+$/.test(text)) { toast('\u5e8f\u53f7\u8bf7\u8f93\u5165\u6570\u5b57'); return false; }
      var n = parseInt(text, 10);
      if (n < 1 || n > 9999) { toast('\u5e8f\u53f7\u9700\u5728 1-9999 \u4e4b\u95f4'); return false; }
      if (n === s.no) return true;
      var dup = null;
      studentsOf(s.classId).forEach(function (x) {
        if (x.id !== s.id && x.no === n) dup = x;
      });
      if (dup) {
        toast('\u5e8f\u53f7 ' + n + ' \u5df2\u88ab' + dup.name + '\u5360\u7528');
        return false;
      }
      s.no = n;
      save();
      toast('\u5b66\u53f7\u5df2\u6539\u4e3a ' + noText(c, s));
      return true;
    });
}

function confirmDelStudent(id) {
  var s = studentById(id);
  if (!s) return;
  var c = classById(s.classId);
  openDialog({ title: '\u5220\u9664' + s.name + '\uff1f',
    text: '\u5b66\u53f7 ' + noText(c, s) + ' \u7684\u5168\u90e8\u4f5c\u4e1a\u8bb0\u5f55\u4f1a\u4e00\u5e76\u5220\u9664\uff0c\u4e0d\u53ef\u64a4\u9500\u3002',
    confirm: '\u5220\u9664', act: 'del-student', id: id });
}

function delStudent(id) {
  store.students = store.students.filter(function (s) { return s.id !== id; });
  Object.keys(store.records).forEach(function (k) {
    if (k.indexOf('|' + id) > 0) delete store.records[k];
  });
  save();
  toast('\u5df2\u5220\u9664');
}

function wipeAll() {
  openDialog({ title: '\u6e05\u7a7a\u5168\u90e8\u6570\u636e\uff1f',
    text: '\u73ed\u7ea7\u3001\u5b66\u751f\u3001\u4f5c\u4e1a\u4e0e\u8bb0\u5f55\u5c06\u5168\u90e8\u5220\u9664\uff0c\u4e0d\u53ef\u64a4\u9500\u3002\u5efa\u8bae\u5148\u5bfc\u51fa\u5907\u4efd\u3002',
    confirm: '\u6e05\u7a7a', act: 'wipe-ok' });
}

/* ---------- 事件层：点击分发 ---------- */

function nowStamp() {
  var d = new Date();
  return pad(d.getMonth() + 1, 2) + '-' + pad(d.getDate(), 2) + ' ' +
    pad(d.getHours(), 2) + ':' + pad(d.getMinutes(), 2);
}

function cloudFileName() {
  var d = new Date();
  return 'hwt-backup-' + d.getFullYear() + pad(d.getMonth() + 1, 2) + pad(d.getDate(), 2) +
    '-' + pad(d.getHours(), 2) + pad(d.getMinutes(), 2) + '.json';
}

function pushCloud() {
  var wd = store.settings.webdav;
  store.cloud.forEach(function (f) { f.latest = false; });
  store.cloud.unshift({
    name: cloudFileName(),
    size: (110 + store.students.length) + ' KB',
    meta: store.classes.length + ' \u73ed / ' + store.students.length + ' \u4eba',
    latest: true
  });
  while (store.cloud.length > wd.keep) store.cloud.pop();
  store.settings.lastBackupAt = nowStamp();
  save();
}

function handle(act, el) {
  var st = store.settings;

  if (PICKERS[act]) {
    if (act === 'wz-col') ui.wizard.colIdx = parseInt(el.getAttribute('data-i'), 10);
    var cfg = PICKERS[act]();
    cfg.act = act;
    openDialog(cfg);
    return;
  }

  switch (act) {
    case 'noop': return;

    case 'tab': goTab(el.getAttribute('data-tab')); return;
    case 'back': back(); return;

    case 'class-tab': ui.classTab = el.getAttribute('data-v'); render(); return;
    case 'open-class':
      var ocid = el.getAttribute('data-id');
      ui.expandedClassId = ui.expandedClassId === ocid ? null : ocid;
      store.currentClassId = ocid;
      save(); render(); return;
    case 'class-edit': classEditMenu(el.getAttribute('data-id')); return;
    case 'cls-recycle': {
      ui.clsRule.recycle = !ui.clsRule.recycle;
      if (el) el.classList.toggle('off', !ui.clsRule.recycle);
      return;
    }
    case 'add-class': addClass(); return;
    case 'add-student': addStudent(); return;
    case 'student-menu': studentMenu(el.getAttribute('data-id')); return;
    case 'del-student': return;
    case 'create-class': createClassFromDraft(); return;
    case 'draft-recycle': draft.recycle = !draft.recycle; render(); return;
    case 'wizard-start': startWizard(); return;

    case 'open-assignment': go('entry', { id: el.getAttribute('data-id') }); return;
    case 'add-assignment': addAssignment(); return;

    case 'cyc-comp': case 'cyc-corr': case 'cyc-grade': {
      var aid = route.params.id, sid = el.getAttribute('data-sid');
      var r = recordOf(aid, sid);
      if (act === 'cyc-comp') writeRecord(aid, sid, { completion: next(COMPLETION, r.completion) });
      else if (act === 'cyc-corr') writeRecord(aid, sid, { correction: next(CORRECTION, r.correction) });
      else writeRecord(aid, sid, { grade: next(GRADES, r.grade) });
      hintLongPress();
      render(); return;
    }
    case 'mark-pick':
      openMarkPicker(el.getAttribute('data-g'), el.getAttribute('data-sid'));
      return;
    case 'bulk-pick':
      togglePick(ui.bulkPick, el.getAttribute('data-g'), el.getAttribute('data-v'));
      render(); return;

    case 'bulk-clear':
      MARK_GROUPS.forEach(function (g) { ui.bulkPick[g.key] = null; });
      render(); return;

    case 'bulk-apply': {
      var a = null;
      store.assignments.forEach(function (x) { if (x.id === route.params.id) a = x; });
      if (!a) return;
      bulkApply(a.id, a.classId);
      render(); return;
    }

    case 'open-scan': go('scan', { aid: route.params.id }); return;
    case 'scan-pick':
      togglePick(ui.scanPick, el.getAttribute('data-g'), el.getAttribute('data-v'));
      render(); return;
    case 'scan-hit': scanHit(); render(); return;

    case 'report-tab': ui.reportTab = el.getAttribute('data-v'); render(); return;
    case 'open-person':
      ui.personId = el.getAttribute('data-id');
      ui.reportTab = 'person';
      goTab('report'); return;
    case 'export-csv': toast('\u5df2\u5bfc\u51fa CSV\uff08\u539f\u578b\u6a21\u62df\uff09'); return;
    case 'export-excel': toast('\u5df2\u5bfc\u51fa Excel\uff08\u539f\u578b\u6a21\u62df\uff09'); return;
    case 'export-one-csv':
    case 'export-one-excel': {
      var ea = currentAssignment();
      if (!ea) return;
      toast('\u5df2\u5bfc\u51fa\u300c' + ea.title + '\u300d\u5b8c\u6210\u60c5\u51b5\u8868' +
        (act === 'export-one-csv' ? ' CSV' : ' Excel') + '\uff08\u539f\u578b\u6a21\u62df\uff09');
      return;
    }
    case 'print': toast('\u5df2\u53d1\u9001\u5230\u6253\u5370\uff08\u539f\u578b\u6a21\u62df\uff09'); return;
    case 'toggle-qrno': st.qrWithNo = !st.qrWithNo; save(); render(); return;

    case 'open-webdav': go('webdav'); return;
    case 'open-qr': go('qr'); return;
    case 'open-backup': go('backup'); return;
    case 'toggle-wd':
      st.webdav.enabled = !st.webdav.enabled;
      if (!st.webdav.enabled) st.webdav.tested = '';
      save(); render(); return;
    case 'toggle-pw': ui.pwVisible = !ui.pwVisible; render(); return;
    case 'wd-test':
      st.webdav.tested = st.webdav.url && st.webdav.user && st.webdav.password ? 'ok' : 'fail';
      save(); render();
      toast(st.webdav.tested === 'ok' ? '\u8fde\u63a5\u6b63\u5e38' : '\u8fde\u63a5\u5931\u8d25\uff1a\u4fe1\u606f\u4e0d\u5b8c\u6574');
      return;
    case 'wd-save': save(); back(); toast('\u5df2\u4fdd\u5b58 WebDAV \u914d\u7f6e'); return;

    case 'toggle-auto': st.autoBackup = !st.autoBackup; save(); render(); return;
    case 'cloud-upload':
      if (!st.webdav.enabled) { toast('\u8bf7\u5148\u542f\u7528 WebDAV'); return; }
      pushCloud(); render(); toast('\u5df2\u4e0a\u4f20\u4e00\u4efd\u5907\u4efd'); return;
    case 'cloud-restore': {
      var f = store.cloud[parseInt(el.getAttribute('data-i'), 10)];
      if (!f) return;
      openDialog({ title: '\u4ece\u4e91\u7aef\u6062\u590d\uff1f', text: f.name + '\n' + f.size + ' \u00b7 ' + f.meta,
        confirm: '\u6062\u590d', act: 'restore-ok' });
      return;
    }
    case 'export-backup': toast('\u5df2\u5bfc\u51fa ' + cloudFileName()); return;
    case 'import-backup':
      openDialog({ title: '\u4ece\u6587\u4ef6\u6062\u590d\uff1f',
        text: '\u5c06\u6821\u9a8c format\uff08hwt-backup\uff09\u4e0e version\uff0c\u6821\u9a8c\u901a\u8fc7\u540e\u8986\u76d6\u5f53\u524d\u672c\u5730\u6570\u636e\u3002',
        confirm: '\u9009\u62e9\u6587\u4ef6', act: 'restore-ok' });
      return;
    case 'restore-ok': return;
    case 'wipe': wipeAll(); return;
    case 'wipe-ok': return;

    case 'wz-pick':
      ui.wizard.picked = true;
      render(); return;
    case 'wz-prev':
      if (ui.wizard.step > 1) ui.wizard.step -= 1; else { ui.wizard = null; back(); return; }
      render(); return;
    case 'wz-next':
      if (ui.wizard.step === 1 && !ui.wizard.picked) { toast('\u8bf7\u5148\u9009\u62e9\u6587\u4ef6'); return; }
      if (ui.wizard.step === 2 && ui.wizard.nameIdx < 0) { toast('\u8bf7\u5148\u6307\u5b9a\u59d3\u540d\u5217'); return; }
      if (ui.wizard.step < 3) ui.wizard.step += 1;
      render(); return;
    case 'wz-commit': commitWizard(); return;

    case 'dlg-cancel': closeDialog(); return;
    case 'dlg-pick': {
      var d = ui.dialog;
      if (!d || !d.options) return;
      var opt = d.options[parseInt(el.getAttribute('data-i'), 10)];
      if (!opt) return;
      if (d.act === 'mark-set') {
        var patch = {};
        patch[d.group] = opt.v;
        writeRecord(route.params.id, d.sid, patch);
      } else {
        applyPick(d.act, opt.v);
      }
      closeDialog();
      render(); return;
    }
    case 'dlg-menu': {
      var dm = ui.dialog;
      if (!dm || !dm.menu) return;
      var mi = dm.menu[parseInt(el.getAttribute('data-i'), 10)];
      if (!mi) return;
      if (dm.menuAct === 'class-edit') classEditPick(mi.act, dm.id);
      else studentMenuPick(mi.act, dm.id);
      return;
    }
    case 'dlg-ok': {
      var dd = ui.dialog;
      if (!dd) return;
      if (dd.act === 'text-ok') {
        var input = document.querySelector('[data-field="dlg-input"]');
        var text = input ? String(input.value).trim() : '';
        if (dd.onOk(text) === false) return;
        closeDialog(); render(); return;
      }
      if (dd.act === 'del-student') { delStudent(dd.id); closeDialog(); render(); return; }
      if (dd.act === 'del-class') {
        var dc = classById(dd.id);
        delClass(dd.id);
        closeDialog(); render();
        toast('\u5df2\u5220\u9664\u73ed\u7ea7\uff1a' + (dc ? dc.name : ''));
        return;
      }
      if (dd.act === 'cls-create') {
        var cn = document.querySelector('[data-field="cls-name"]');
        var cname = cn ? String(cn.value).trim() : '';
        if (!cname) { toast('\u8bf7\u8f93\u5165\u73ed\u7ea7\u540d\u79f0'); return; }
        var cpre = document.querySelector('[data-field="cls-prefix"]');
        var cdig = document.querySelector('[data-field="cls-digits"]');
        var cd = parseInt(cdig && cdig.value, 10);
        if (!(cd >= 1 && cd <= 4)) cd = 2;
        var nc = { id: uid('c'), name: cname, note: '',
          prefix: cpre ? String(cpre.value).trim() : '', digits: cd, recycle: !!ui.clsRule.recycle };
        store.classes.push(nc);
        store.currentClassId = nc.id;
        save();
        ui.classTab = 'class';
        ui.expandedClassId = nc.id;
        closeDialog(); render();
        toast('\u5df2\u521b\u5efa\uff1a' + cname);
        return;
      }
      if (dd.act === 'cls-rule-save') {
        var cc = classById(dd.id);
        if (cc) {
          var rpre = document.querySelector('[data-field="cls-prefix"]');
          var rdig = document.querySelector('[data-field="cls-digits"]');
          var rd = parseInt(rdig && rdig.value, 10);
          if (!(rd >= 1 && rd <= 4)) rd = 2;
          cc.prefix = rpre ? String(rpre.value).trim() : '';
          cc.digits = rd;
          cc.recycle = !!ui.clsRule.recycle;
          save();
          closeDialog(); render();
          toast('\u5b66\u53f7\u89c4\u5219\u5df2\u4fdd\u5b58\uff1a' + noText(cc, { no: 1 }) + '-' + noText(cc, { no: 2 }) + '\u2026');
        }
        return;
      }
      if (dd.act === 'renumber-ok') { renumberClass(dd.id); closeDialog(); render(); return; }
      if (dd.act === 'wipe-ok') {
        store = emptyStore(); save();
        closeDialog();
        stack = [];
        replace('onboard');
        toast('\u6570\u636e\u5df2\u6e05\u7a7a');
        return;
      }
      if (dd.act === 'restore-ok') {
        closeDialog();
        toast('\u5df2\u6062\u590d\uff08\u539f\u578b\u6a21\u62df\uff0c\u672a\u8986\u76d6\u6570\u636e\uff09');
        return;
      }
      closeDialog(); return;
    }

    case 'reset-demo':
      store = seedStore(); save();
      ui.wizard = null; ui.personId = null; stack = [];
      replace('homework');
      toast('\u5df2\u91cd\u7f6e\u4e3a\u6f14\u793a\u6570\u636e'); return;
    case 'reset-onboard':
      store.onboarded = false; save();
      stack = [];
      replace('onboard'); return;

    default: return;
  }
}

/* ---------- 绑定与启动 ---------- */

function closestAttr(node, attr) {
  while (node && node !== document) {
    if (node.getAttribute && node.getAttribute(attr) != null) return node;
    node = node.parentNode;
  }
  return null;
}

function onClick(e) {
  if (longFiredAt) {
    var stale = Date.now() - longFiredAt > LONG_SUPPRESS;
    longFiredAt = 0;
    if (!stale) return;
  }
  var el = closestAttr(e.target, 'data-act');
  if (!el) return;
  var stopper = closestAttr(e.target, 'data-stop');
  if (stopper && el.contains(stopper) && el !== stopper) return;
  handle(el.getAttribute('data-act'), el);
}

/* ---------- 长按通道（单击循环 + 长按直选） ---------- */

var LONG_MS = 420, LONG_MOVE = 10, LONG_SUPPRESS = 700;
var longTimer = null, longFiredAt = 0, longEl = null, longX = 0, longY = 0;

function longClear() {
  if (longTimer) { clearTimeout(longTimer); longTimer = null; }
  if (longEl) { longEl.classList.remove('long-press'); longEl = null; }
}

function longStart(e) {
  if (e.type === 'mousedown' && e.button !== 0) return;
  longClear();
  longFiredAt = 0;
  var el = closestAttr(e.target, 'data-long');
  if (!el) return;
  var pt = e.touches && e.touches[0] ? e.touches[0] : e;
  longEl = el;
  longX = pt.clientX;
  longY = pt.clientY;
  el.classList.add('long-press');
  longTimer = setTimeout(function () {
    var target = longEl;
    longClear();
    if (!target) return;
    longFiredAt = Date.now();
    handle(target.getAttribute('data-long'), target);
  }, LONG_MS);
}

function longMove(e) {
  if (!longEl) return;
  var pt = e.touches && e.touches[0] ? e.touches[0] : e;
  if (Math.abs(pt.clientX - longX) > LONG_MOVE || Math.abs(pt.clientY - longY) > LONG_MOVE) longClear();
}

function bindLong(host) {
  host.addEventListener('mousedown', longStart);
  host.addEventListener('mousemove', longMove);
  host.addEventListener('mouseup', longClear);
  host.addEventListener('mouseleave', longClear);
  host.addEventListener('touchstart', longStart);
  host.addEventListener('touchmove', longMove);
  host.addEventListener('touchend', longClear);
  host.addEventListener('touchcancel', longClear);
  host.addEventListener('contextmenu', function (e) {
    if (closestAttr(e.target, 'data-long')) e.preventDefault();
  });
  host.addEventListener('scroll', longClear, true);
}

function refreshHint() {
  var hint = document.querySelector('.onboard .field-hint');
  if (!hint) return;
  var d = parseInt(draft.digits, 10);
  if (!(d >= 1 && d <= 4)) d = 2;
  hint.textContent = '\u793a\u4f8b\uff1a' + [1, 2, 3].map(function (n) {
    return (draft.prefix || '') + pad(n, d);
  }).join('\u3001') + '\u2026';
}

function onInput(e) {
  var t = e.target;
  if (!t || !t.getAttribute) return;
  var f = t.getAttribute('data-field');
  if (!f) return;
  var v = t.value;
  if (f === 'name') { draft.name = v; return; }
  if (f === 'note') { draft.note = v; return; }
  if (f === 'prefix') { draft.prefix = v; refreshHint(); return; }
  if (f === 'digits') { draft.digits = v; refreshHint(); return; }
  if (f === 'cls-prefix' || f === 'cls-digits') { refreshClsSample(); return; }
  if (f === 'wd-url') { store.settings.webdav.url = v; store.settings.webdav.tested = ''; save(); return; }
  if (f === 'wd-user') { store.settings.webdav.user = v; store.settings.webdav.tested = ''; save(); return; }
  if (f === 'wd-pass') { store.settings.webdav.password = v; store.settings.webdav.tested = ''; save(); return; }
}

function onKeydown(e) {
  if (e.key !== 'Enter') return;
  if (!e.target || !e.target.getAttribute) return;
  if (e.target.getAttribute('data-field') !== 'dlg-input') return;
  var ok = document.querySelector('[data-act="dlg-ok"]');
  if (ok) ok.click();
}

var phone = document.getElementById('phone');
if (phone) { phone.addEventListener('click', onClick); bindLong(phone); }
var side = document.querySelector('.proto-side');
if (side) side.addEventListener('click', onClick);
document.addEventListener('input', onInput);
document.addEventListener('keydown', onKeydown);

load();
route = { name: store.onboarded ? 'homework' : 'onboard', params: {} };
stack = [];
render();
})();
