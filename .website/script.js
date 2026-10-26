// Mobile navigation toggle, same behaviour as the other PiBOH project pages.
(function () {
  var toggle = document.querySelector('.menu-toggle');
  var nav = document.getElementById('primary-nav');
  if (!toggle || !nav) return;

  toggle.addEventListener('click', function () {
    var open = nav.classList.toggle('is-open');
    toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
  });
})();
