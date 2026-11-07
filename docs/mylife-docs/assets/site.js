// MyLife 文档站点 · 交互脚本：自动目录、滚动高亮、导航开关、Mermaid 配置
(function () {
  'use strict';

  // ---------- 移动端导航开关 ----------
  var toggle = document.querySelector('.nav-toggle');
  var topnav = document.querySelector('.topnav');
  if (toggle && topnav) {
    toggle.addEventListener('click', function () {
      topnav.classList.toggle('open');
    });
  }

  // ---------- Mermaid 配置（颜色取自 CSS 变量，与主题一致） ----------
  if (window.mermaid && document.querySelector('.mermaid')) {
    var root = getComputedStyle(document.documentElement);
    var pick = function (name, fallback) {
      var v = root.getPropertyValue(name).trim();
      return v || fallback;
    };

    mermaid.initialize({
      startOnLoad: true,
      securityLevel: 'loose',
      theme: 'base',
      fontFamily: getComputedStyle(document.body).fontFamily,
      flowchart: {
        curve: 'basis',
        nodeSpacing: 42,
        rankSpacing: 56,
        useMaxWidth: true,
        htmlLabels: false
      },
      themeVariables: {
        primaryColor: pick('--bg2', '#E7EEE2'),
        primaryTextColor: pick('--ink', '#2A332E'),
        primaryBorderColor: pick('--accent', '#487A5C'),
        lineColor: pick('--accent2', '#3F7267'),
        secondaryColor: pick('--surface-solid', '#FBFCF9'),
        tertiaryColor: pick('--bg', '#F2F6EF'),
        mainBkg: pick('--surface-solid', '#FBFCF9'),
        background: 'transparent',
        edgeLabelBackground: pick('--surface-solid', '#FBFCF9'),
        clusterBkg: pick('--surface', 'rgba(255,255,255,0.6)'),
        clusterBorder: pick('--rule', '#D3DDCD'),
        nodeTextColor: pick('--ink', '#2A332E')
      }
    });
  }

  // ---------- 自动生成页面目录 ----------
  var main = document.querySelector('main.content');
  var tocNav = document.getElementById('toc-desktop');
  var tocMobile = document.getElementById('toc-mobile');
  if (main && (tocNav || tocMobile)) {
    var heads = main.querySelectorAll('h2, h3');
    var links = [];

    heads.forEach(function (h, i) {
      var id = 'sec-' + (i + 1);
      h.id = id;
      var item = document.createElement('a');
      item.href = '#' + id;
      item.className = 'toc-link ' + (h.tagName === 'H3' ? 'toc-h3' : 'toc-h2');
      item.textContent = h.textContent;
      links.push(item);
      if (tocNav) tocNav.appendChild(item);
      if (tocMobile) tocMobile.appendChild(item.cloneNode(true));
    });

    // 滚动高亮当前章节
    if ('IntersectionObserver' in window && links.length) {
      var current = null;
      var observer = new IntersectionObserver(function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) current = entry.target.id;
        });
        if (!current) return;
        links.forEach(function (a) {
          a.classList.toggle('active', a.getAttribute('href') === '#' + current);
        });
        if (tocMobile) {
          var activeMobile = tocMobile.querySelector('a.active');
          if (activeMobile) activeMobile.scrollIntoView({ block: 'nearest' });
        }
      }, { rootMargin: '-90px 0px -70% 0px', threshold: 0 });

      heads.forEach(function (h) { observer.observe(h); });
    }
  }
})();