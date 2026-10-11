const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 3000;
const MIME = {
  '.html': 'text/html',
  '.css': 'text/css',
  '.js': 'application/javascript',
  '.json': 'application/json',
  '.svg': 'image/svg+xml'
};

const server = http.createServer((req, res) => {
  let safePath = req.url.split('?')[0];
  let file = safePath === '/' ? '/index.html' : safePath;
  let filePath = path.join(__dirname, file);
  let ext = path.extname(filePath);

  fs.readFile(filePath, (err, data) => {
    if (err) {
      res.writeHead(404, { 'Content-Type': 'text/plain' });
      res.end('Not found: ' + req.url);
    } else {
      res.writeHead(200, {
        'Content-Type': MIME[ext] || 'text/plain',
        'Access-Control-Allow-Origin': '*'
      });
      res.end(data);
    }
  });
});

server.listen(PORT, () => {
  console.log(`Enterprise Dashboard running at http://localhost:${PORT}`);
});
