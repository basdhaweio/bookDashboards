/**
 * Book Notification → bookInbox forwarder (Google Apps Script).
 *
 * Runs INSIDE your Gmail account: nothing to forward anywhere. It finds the
 * weekly Book Notification emails and writes each one, raw, as an
 * `email_forward` event into the bookInbox repo — the same GitHub contents
 * API the dashboard's Log view uses. Jerry parses them into the Book Club
 * feed (and proposals); the parsing never lives here.
 *
 * ONE-TIME SETUP
 *   1. script.google.com → New project → replace the default code with this
 *      file → name it "bn forwarder".
 *   2. Project Settings (gear) → Script Properties → Add:
 *        GITHUB_TOKEN  a fine-grained personal access token with
 *                      Repository access: basdhaweio/bookInbox only,
 *                      Permissions: Contents → Read and write. Nothing else.
 *        GITHUB_REPO   basdhaweio/bookInbox        (optional — the default)
 *        BN_QUERY      from:booknotification.com  (optional — the default;
 *                      change only if their sender domain differs)
 *   3. Pick `forwardNow` in the function dropdown → Run. Google asks you to
 *      authorize Gmail (read/modify labels) and external requests; allow.
 *      It back-fills up to 90 days of digests; each forwarded thread gets
 *      the Gmail label `bn-forwarded`, so nothing is ever sent twice.
 *   4. Pick `installTrigger` → Run once. From then on it runs every
 *      Wednesday at 06:00 (the digest lands Tuesday).
 *
 * The event shape mirrors the dashboard's: {v, id, at, by, type, payload}.
 */
var LABEL = 'bn-forwarded';
var DEFAULT_REPO = 'basdhaweio/bookInbox';
var DEFAULT_QUERY = 'from:booknotification.com';

function prop(name, fallback) {
  var v = PropertiesService.getScriptProperties().getProperty(name);
  return v ? v : fallback;
}

function forwardNow() {
  var token = prop('GITHUB_TOKEN', '');
  if (!token) throw new Error('Set GITHUB_TOKEN in Script Properties first');
  var repo = prop('GITHUB_REPO', DEFAULT_REPO);
  var query = prop('BN_QUERY', DEFAULT_QUERY) + ' -label:' + LABEL + ' newer_than:90d';
  var label = GmailApp.getUserLabelByName(LABEL) || GmailApp.createLabel(LABEL);
  var sent = 0;
  GmailApp.search(query, 0, 50).forEach(function (thread) {
    thread.getMessages().forEach(function (msg) {
      var ev = {
        v: 1,
        id: Utilities.getUuid().replace(/-/g, '').slice(0, 12),
        at: new Date().toISOString(),
        by: 'butthead',
        type: 'email_forward',
        bundle_generated_at: null,
        payload: {
          from: msg.getFrom(),
          subject: msg.getSubject(),
          date: msg.getDate().toISOString(),
          message_id: msg.getId(),
          html: msg.getBody(),
          text: msg.getPlainBody()
        }
      };
      var name = ev.at.replace(/[:.]/g, '-') + '-email_forward-' + ev.id.slice(-4) + '.json';
      var res = UrlFetchApp.fetch('https://api.github.com/repos/' + repo + '/contents/inbox/' + name, {
        method: 'put',
        contentType: 'application/json',
        headers: {
          Authorization: 'Bearer ' + token,
          Accept: 'application/vnd.github+json',
          'X-GitHub-Api-Version': '2022-11-28'
        },
        payload: JSON.stringify({
          message: 'email_forward: ' + msg.getSubject(),
          content: Utilities.base64Encode(JSON.stringify(ev), Utilities.Charset.UTF_8)
        }),
        muteHttpExceptions: true
      });
      if (res.getResponseCode() >= 300) {
        throw new Error('GitHub ' + res.getResponseCode() + ': ' + res.getContentText().slice(0, 200));
      }
      sent++;
    });
    thread.addLabel(label);
  });
  Logger.log('forwarded ' + sent + ' message(s)');
  return sent;
}

function installTrigger() {
  ScriptApp.getProjectTriggers()
    .filter(function (t) { return t.getHandlerFunction() === 'forwardNow'; })
    .forEach(function (t) { ScriptApp.deleteTrigger(t); });
  ScriptApp.newTrigger('forwardNow').timeBased()
    .onWeekDay(ScriptApp.WeekDay.WEDNESDAY).atHour(6).create();
  Logger.log('weekly trigger installed: Wednesdays 06:00');
}
