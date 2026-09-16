const fs = require('fs');
const path = require('path');

const distDir = path.join(__dirname, 'dist');
if (!fs.existsSync(distDir)) {
  fs.mkdirSync(distDir, { recursive: true });
}

function copyRecursiveSync(src, dest) {
  const exists = fs.existsSync(src);
  const stats = exists && fs.statSync(src);
  const isDirectory = exists && stats.isDirectory();
  if (isDirectory) {
    if (!fs.existsSync(dest)) {
      fs.mkdirSync(dest, { recursive: true });
    }
    fs.readdirSync(src).forEach((childItemName) => {
      copyRecursiveSync(path.join(src, childItemName), path.join(dest, childItemName));
    });
  } else if (exists) {
    fs.copyFileSync(src, dest);
  }
}

const itemsToCopy = [
  'index.html',
  'features.html',
  'pricing.html',
  'about.html',
  'privacy.html',
  'terms.html',
  'refund.html',
  'guides.html',
  'googlefizlBwqvFqvA47dXrpi7GA3AOoU08X4Xrz-o9q7nONU.html',
  'assets',
  'guides',
  'ASAR-Latest.apk',
  'ASAR-v1.0.0.apk'
];

itemsToCopy.forEach((item) => {
  const srcPath = path.join(__dirname, item);
  const destPath = path.join(distDir, item);
  if (fs.existsSync(srcPath)) {
    copyRecursiveSync(srcPath, destPath);
    console.log(`Copied ${item} to dist/`);
  }
});

console.log('Build complete! Successfully generated dist/ directory.');
