const fs = require('fs');
const path = require('path');

const docsDir = __dirname;
const outDir = path.join(docsDir, 'pdf-ready');

if (!fs.existsSync(outDir)) fs.mkdirSync(outDir);

const CSS = [
  'body { font-family: Segoe UI, -apple-system, sans-serif; max-width: 900px; margin: 2rem auto; padding: 0 2rem; color: #1a1a2e; line-height: 1.7; }',
  'h1 { color: #4f46e5; border-bottom: 3px solid #4f46e5; padding-bottom: 0.5rem; margin-top: 2rem; }',
  'h2 { color: #6366f1; border-bottom: 1px solid #e5e7eb; padding-bottom: 0.3rem; margin-top: 1.5rem; }',
  'h3 { color: #7c3aed; margin-top: 1.2rem; }',
  'code { background: #f3f4f6; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }',
  'pre { background: #1e1e2e; color: #cdd6f4; padding: 1rem; border-radius: 8px; overflow-x: auto; font-size: 0.85rem; }',
  'pre code { background: none; padding: 0; color: inherit; }',
  'table { border-collapse: collapse; width: 100%; margin: 1rem 0; }',
  'th, td { border: 1px solid #d1d5db; padding: 0.5rem 0.75rem; text-align: left; font-size: 0.9rem; }',
  'th { background: #f3f4f6; font-weight: 600; }',
  'tr:nth-child(even) { background: #f9fafb; }',
  'blockquote { border-left: 4px solid #4f46e5; padding-left: 1rem; color: #6b7280; margin: 1rem 0; }',
  'hr { border: none; border-top: 2px solid #e5e7eb; margin: 2rem 0; }',
  '@media print { body { max-width: 100%; margin: 0; } pre { white-space: pre-wrap; } }'
].join('\n');

function escapeHtml(text) {
  return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function mdToHtml(md) {
  var html = md;

  // Code blocks
  html = html.replace(/```[\w]*\n([\s\S]*?)```/g, function(match, code) {
    return '<pre><code>' + escapeHtml(code.trim()) + '</code></pre>';
  });

  // Inline code
  html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

  // Headings
  html = html.replace(/^#### (.+)$/gm, '<h4>$1</h4>');
  html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
  html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
  html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');

  // Bold & italic
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');

  // Horizontal rules
  html = html.replace(/^---$/gm, '<hr>');

  // Lists
  html = html.replace(/^- (.+)$/gm, '<li>$1</li>');

  // Paragraphs for non-tag lines
  var lines = html.split('\n');
  var out = [];
  for (var i = 0; i < lines.length; i++) {
    var line = lines[i].trim();
    if (line === '') { out.push(''); continue; }
    if (line.startsWith('<')) { out.push(line); continue; }
    if (line.startsWith('|')) {
      // Table row
      var cells = line.split('|').filter(function(c) { return c.trim(); });
      if (line.match(/^[\s|:-]+$/)) continue; // separator
      out.push('<tr>' + cells.map(function(c) { return '<td>' + c.trim() + '</td>'; }).join('') + '</tr>');
      continue;
    }
    out.push('<p>' + line + '</p>');
  }

  return out.join('\n');
}

var mdFiles = fs.readdirSync(docsDir).filter(function(f) { return f.endsWith('.md'); });

mdFiles.forEach(function(file) {
  var md = fs.readFileSync(path.join(docsDir, file), 'utf-8');
  var title = file.replace('.md', '').replace(/_/g, ' ');
  var today = new Date().toISOString().split('T')[0];

  var htmlContent = '<!DOCTYPE html>\n<html><head>\n<meta charset="UTF-8">\n<title>LocalLens - ' + title + '</title>\n<style>' + CSS + '</style>\n</head><body>\n' + mdToHtml(md) + '\n<footer style="margin-top:3rem;padding-top:1rem;border-top:1px solid #e5e7eb;color:#9ca3af;font-size:0.8rem;text-align:center;">LocalLens Architecture Documentation - Generated ' + today + '</footer>\n</body></html>';

  var outFile = path.join(outDir, file.replace('.md', '.html'));
  fs.writeFileSync(outFile, htmlContent);
  console.log('Created: ' + outFile);
});

console.log('\nDone! Open the HTML files in Chrome and use Ctrl+P to save as PDF.');
console.log('Files are in: ' + outDir);
