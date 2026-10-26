# github-pages

Project website for [CleanMCMod](https://github.com/PiBOH/cleanmcmod), served by
GitHub Pages from this branch.

- `.website/` is the whole site: three pages (`index.html`, `install.html`,
  `versions.html`) plus a 404 page, one stylesheet, one small script and the
  logo. It is plain HTML with no build step and no dependency.
- `.github/workflows/deploy.yml` publishes `.website/` with
  `actions/deploy-pages` on every push to this branch.

GitHub needs to know the site comes from a workflow: **Settings → Pages →
Source → GitHub Actions**. The published URL is then
<https://piboh.github.io/cleanmcmod/>.

The mod itself, its build and its release branches live on `main` and on the
per-version branches (`1.21.8` … `26.3`); nothing in this branch is compiled or
shipped in the jar.
