import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
import { fileURLToPath } from 'node:url';

const dir = path.dirname(fileURLToPath(import.meta.url));
const read = p => JSON.parse(fs.readFileSync(p, 'utf8'));
const manifest = read(path.join(dir, 'coverage.json'));
const sources = manifest.sources.map(s => read(path.join(dir, s.path)));
const [female, male] = sources;
const sourceCards = sources.flatMap(d => d.exerciseSections.flatMap(s => s.exercises));
const keys = o => Object.keys(o);
const withoutSections = d => Object.fromEntries(Object.entries(d).filter(([k]) => k !== 'exerciseSections'));
const canon = v => JSON.stringify(v);
const allowedRedirects = new Set(sourceCards.map(c => canon(c.redirect)));
const allowedStartScreens = new Set(sourceCards.filter(c => c.startScreen).map(c => canon(c.startScreen)));
const allowedImages = new Set(sourceCards.map(c => c.imageURL));
const allowedProducts = new Set(sourceCards.filter(c => c.product).map(c => canon(c.product)));
const supported = new Set(manifest.observedLayouts);
const expectedCounts = [[4,30],[8,51],[6,12],[6,24],[6,12],[6,12],[6,60],[1,1],[0,0],[6,12],[6,6]];
const payloads = [];
let cardCount = 0;

for (const [i, fixture] of manifest.fixtures.entries()) {
  const d = read(path.join(dir, fixture.file));
  payloads.push(d);
  assert.deepEqual(withoutSections(d), withoutSections(female), fixture.file + ': female root');
  assert.deepEqual(keys(d).sort(), keys(female).sort(), fixture.file + ': root schema');
  const cards = d.exerciseSections.flatMap(s => s.exercises);
  assert.deepEqual([d.exerciseSections.length,cards.length], expectedCounts[i], fixture.file + ': counts');
  const sectionTitles = new Set();
  for (const s of d.exerciseSections) {
    assert(s.title && typeof s.title === 'string', fixture.file + ': section title');
    assert(!sectionTitles.has(s.title), fixture.file + ': duplicate section');
    sectionTitles.add(s.title);
    assert(supported.has(s.layout), fixture.file + ': layout');
    assert(Array.isArray(s.exercises), fixture.file + ': exercises');
    if (s.order !== undefined) assert(Number.isInteger(s.order), fixture.file + ': order type');
    for (const c of s.exercises) {
      assert(c.title && typeof c.title === 'string', fixture.file + ': title');
      assert(allowedImages.has(c.imageURL), fixture.file + ': unknown image');
      assert.equal(keys(c.redirect).length, 1, fixture.file + ': exactly one redirect');
      assert(allowedRedirects.has(canon(c.redirect)), fixture.file + ': modified or invented redirect');
      if (c.startScreen) assert(allowedStartScreens.has(canon(c.startScreen)), fixture.file + ': modified startScreen');
      if (c.product) assert(allowedProducts.has(canon(c.product)), fixture.file + ': modified/invented product');
      if (s.layout === 'challenge_card') assert(typeof c.description === 'string' && c.description.length > 0);
      if (s.layout === 'coaching_session_card') {
        assert(typeof c.subtitle === 'string' && c.subtitle.length > 0);
        assert(typeof c.buttonTitle === 'string' && c.buttonTitle.length > 0);
      }
      for (const k of ['imageURL']) assert.equal(new URL(c[k]).protocol, 'https:');
    }
  }
  cardCount += cards.length;
}
assert.deepEqual(payloads[0], female, 'baseline exact equality');
const combinedExpected = [...male.exerciseSections, ...female.exerciseSections].map((s,i) => ({...s, order:i+1}));
assert.deepEqual(payloads[1].exerciseSections, combinedExpected, 'all original source sections/cards retained');
assert.deepEqual(new Set(payloads[1].exerciseSections.map(s => s.layout)), supported);
assert.deepEqual(new Set(payloads[2].exerciseSections.map(s => s.layout)), supported);
assert.equal(payloads[3].exerciseSections.length, 6);
const flow = c => c.redirect.coach_video ? 'video' : c.redirect.web_view ? 'webview'
  : c.redirect.coach_lesson?.feedback_id === 'feedback_explore_practice' ? 'practice' : 'lesson';
for (const s of payloads[3].exerciseSections) {
  assert.equal(s.exercises.length, 4);
  assert.deepEqual(new Set(s.exercises.map(flow)), new Set(['lesson','practice','video','webview']));
  for (const c of s.exercises) {
    if (['practice','video'].includes(flow(c))) assert(c.startScreen);
    assert(!c.product && c.redirect.kegel_workout === undefined, 'matrix isolates male-only access');
  }
}
assert.equal(manifest.navigationCases.length, 24);
assert.deepEqual(
  manifest.navigationCases.map(c => [c.layout,c.cardTitle,c.redirect,c.startScreen]),
  payloads[3].exerciseSections.flatMap(s => s.exercises.map(c => [s.layout,c.title,c.redirect,c.startScreen ?? null]))
);
assert.deepEqual(payloads[4].exerciseSections,
  [...payloads[2].exerciseSections].reverse().map((s,i) => ({...s,order:i+1,exercises:[...s.exercises].reverse()})));
for (const [i,s] of payloads[5].exerciseSections.entries()) {
  for (const [j,c] of s.exercises.entries()) {
    const original = payloads[2].exerciseSections[i].exercises[j];
    assert.deepEqual(c.redirect, original.redirect, 'text/image changes preserve destination');
    assert.deepEqual(c.startScreen, original.startScreen, 'text/image changes preserve startScreen');
    assert.notEqual(c.imageURL, original.imageURL, 'image change is effective');
    assert.notEqual(c.title, original.title, 'title change is effective');
  }
}
for (const s of payloads[6].exerciseSections) {
  assert.equal(s.exercises.length,10);
  assert.equal(new Set(s.exercises.map(c => c.title)).size,10);
}
assert.equal(payloads[7].exerciseSections[0].layout,'coaching_session_card');
assert(payloads[7].exerciseSections[0].exercises[0].redirect.coach_lesson);
assert.deepEqual(payloads[8].exerciseSections,[]);
assert.deepEqual(payloads[9].exerciseSections.map(s => s.layout), manifest.observedLayouts);
assert.deepEqual(payloads[9].exerciseSections.map(s => s.order),[60,50,40,30,20,10]);
for (const s of payloads[10].exerciseSections) {
  assert(s.exercises[0].redirect.coach_video);
  assert.equal(s.exercises[0].startScreen, undefined);
}
console.log(JSON.stringify({
  status:'PASS',payloads:payloads.length,cardInstances:cardCount,
  layouts:manifest.observedLayouts,navigationCombinations:manifest.navigationCases.length,
  checks:['JSON syntax','female root','source baseline equality','51 unchanged source cards',
    'known literal layouts','source-only redirects/images/startScreens/products',
    '6 x 4 navigation matrix','reorder invariants','text/image change invariants',
    '0/1/10-card fixtures','exploratory fixtures separate'],
  iosRuntime:'NOT RUN',visualComparison:'NOT RUN'
},null,2));
