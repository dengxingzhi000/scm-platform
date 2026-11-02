/* SCM Platform — official site behaviour.
   Progressive enhancement only: every page remains readable without JS. */
(function () {
  'use strict';

  document.documentElement.classList.add('js');

  /* Reveal on scroll ------------------------------------------------------ */
  var revealables = Array.prototype.slice.call(document.querySelectorAll('[data-reveal]'));

  if (revealables.length) {
    if ('IntersectionObserver' in window) {
      var groups = new Map();
      revealables.forEach(function (el) {
        var parent = el.parentElement;
        var list = groups.get(parent) || [];
        list.push(el);
        groups.set(parent, list);
        var index = list.length - 1;
        el.style.setProperty('--delay', Math.min(index, 6) * 70 + 'ms');
      });

      var observer = new IntersectionObserver(
        function (entries) {
          entries.forEach(function (entry) {
            if (!entry.isIntersecting) return;
            entry.target.classList.add('is-in');
            observer.unobserve(entry.target);
          });
        },
        { rootMargin: '0px 0px -8% 0px', threshold: 0.12 }
      );
      revealables.forEach(function (el) { observer.observe(el); });
    } else {
      revealables.forEach(function (el) { el.classList.add('is-in'); });
    }
  }

  /* Sticky header state --------------------------------------------------- */
  var header = document.querySelector('.site-header');
  var progress = document.querySelector('.progress span');

  function onScroll() {
    var y = window.scrollY || window.pageYOffset;
    if (header) header.classList.toggle('is-scrolled', y > 8);

    if (progress) {
      var height = document.documentElement.scrollHeight - window.innerHeight;
      var ratio = height > 0 ? Math.min(1, Math.max(0, y / height)) : 0;
      progress.style.width = (ratio * 100).toFixed(2) + '%';
    }

    /* If a jump scrolls past a section, reveal anything already above the
       viewport so it never comes back blank. */
    revealables.forEach(function (el) {
      if (!el.classList.contains('is-in') && el.getBoundingClientRect().bottom < 0) {
        el.classList.add('is-in');
      }
    });
  }
  onScroll();
  window.addEventListener('scroll', onScroll, { passive: true });
  window.addEventListener('resize', onScroll, { passive: true });

  /* Philosophy filters ---------------------------------------------------- */
  var filters = Array.prototype.slice.call(document.querySelectorAll('[data-filter]'));
  var principles = Array.prototype.slice.call(document.querySelectorAll('[data-tags]'));

  function applyFilter(tag) {
    principles.forEach(function (section) {
      var tags = (section.getAttribute('data-tags') || '').split(/\s+/);
      section.hidden = tag !== 'all' && tags.indexOf(tag) === -1;
    });
    filters.forEach(function (button) {
      button.setAttribute('aria-pressed', String(button.getAttribute('data-filter') === tag));
    });
  }

  if (filters.length && principles.length) {
    filters.forEach(function (button) {
      button.addEventListener('click', function () {
        applyFilter(button.getAttribute('data-filter'));
      });
    });
    applyFilter('all');
  }

  /* Portal language suggestion ------------------------------------------- */
  var doors = document.querySelectorAll('[data-lang-door]');
  if (doors.length) {
    var lang = (navigator.language || navigator.userLanguage || '').toLowerCase();
    var preferred = lang.indexOf('zh') === 0 ? 'zh' : 'en';
    Array.prototype.forEach.call(doors, function (door) {
      door.classList.toggle('is-suggested', door.getAttribute('data-lang-door') === preferred);
    });
  }
})();
