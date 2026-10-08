# Changelog

## [3.4.0](https://github.com/Doggo785/evoe/compare/v3.3.0...v3.4.0) (2026-10-08)


### Features

* **deezer:** serve all likes screens from one snapshot fetch ([c5b1527](https://github.com/Doggo785/evoe/commit/c5b152794244740b0082d471b7d54671ce091c76))
* **likes:** include featuring appearances in artist liked tracks ([1e058b1](https://github.com/Doggo785/evoe/commit/1e058b12246488737fdaaad70ad31e4a846717f2))
* **updater:** ask before installing app updates, with changelog ([d999401](https://github.com/Doggo785/evoe/commit/d999401dbf63cf74024e18d20b54e85314f79cbc))


### Bug Fixes

* **deezer:** resolve loved playlist card to the real entry ([4323cda](https://github.com/Doggo785/evoe/commit/4323cdafd9d0a17007f1b44a8c9a8edcbe542994))
* **deezer:** step down quality before fallback track ([b670a18](https://github.com/Doggo785/evoe/commit/b670a189287b90853cb482b33eb2def460046d14))
* **feed:** restore see-all content after process death ([27eac71](https://github.com/Doggo785/evoe/commit/27eac71021a18c8be65763adaa912988ac3a570f))
* **player:** hold on Deezer stalls instead of skipping ([6f644b1](https://github.com/Doggo785/evoe/commit/6f644b1091c9cb9681e08fe0d87833d32f081e66))
* **player:** verify likes on auto-advance and re-enable the heart ([3b385d2](https://github.com/Doggo785/evoe/commit/3b385d2ae60770875f87518599331453ba304c12))
* **playlist:** edit drag and remove crash ([27eacc5](https://github.com/Doggo785/evoe/commit/27eacc53d9d8fe9455b1502a5bfe63c15f63c633))

## [3.3.0](https://github.com/Doggo785/evoe/compare/v3.2.0...v3.3.0) (2026-10-07)


### Features

* **player:** start playback without waiting for likes ([658fc80](https://github.com/Doggo785/evoe/commit/658fc80db590c4417107bdf2c11d416c1b50a547))


### Bug Fixes

* **extensions:** register file pickers before fragment creation ([b10b326](https://github.com/Doggo785/evoe/commit/b10b326c909aafad42db29a27f379b035bde92fa))

## [3.2.0](https://github.com/Doggo785/evoe/compare/v3.1.0...v3.2.0) (2026-10-04)


### Features

* **build:** source versionName from version.txt ([502d625](https://github.com/Doggo785/evoe/commit/502d625119c3195c46fd38a728e20a64c585d04f))
* **player:** announce player likes with a message ([26d1cd8](https://github.com/Doggo785/evoe/commit/26d1cd8c4602982bbfaf3cc4159133ba42c26b0d))
* **player:** back-swipe to previous track with animation, retry blank covers ([38b0db4](https://github.com/Doggo785/evoe/commit/38b0db4d911d93896aa7e9bea438adef9e8eaf37))
* **player:** replace wavy seek bar with straight slider bar ([ebdeac2](https://github.com/Doggo785/evoe/commit/ebdeac2a37d8c47895d949e070c127a420da12bc))
* **telemetry:** sample peak memory every 60s ([2461280](https://github.com/Doggo785/evoe/commit/24612808ee04c4a819b8795bc970323f85173022))
* **updater:** offer app updates by semver comparison ([#13](https://github.com/Doggo785/evoe/issues/13)) ([e32cbf1](https://github.com/Doggo785/evoe/commit/e32cbf1ba02734c2a64ea6f7bab13d03d1e49a35))


### Bug Fixes

* **app:** jump fast-scroller thumb drags to position instead of scrolling every row ([f9ed000](https://github.com/Doggo785/evoe/commit/f9ed000688a9fe109d66cf772352e318a03fcfc9))
* **app:** move cache decode and page loads off main ([5c60f3e](https://github.com/Doggo785/evoe/commit/5c60f3ebfd79c7dc69206fdbf4b8a52a2cb724bb))
* **build:** make release-please run without a v3.1.0 tag ([ee92ccf](https://github.com/Doggo785/evoe/commit/ee92ccf238498a0fd066f613901383c7fcfaf4e7))
* **player:** stop phantom likes from heart state restores ([cc9473e](https://github.com/Doggo785/evoe/commit/cc9473eca8f67b64c1bbeab0ce265678c7bd27b5))
* **quality:** analyze :common with detekt and pin its baseline ([de69891](https://github.com/Doggo785/evoe/commit/de6989123ec2b0d9c745ab05b86d033080fe5ee2))
* **settings:** hide file pickers where none exists, explain when tapped ([6f495bb](https://github.com/Doggo785/evoe/commit/6f495bb8868332a456c4e98e7c56300645a1a2f9))
