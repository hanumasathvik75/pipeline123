import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';

const __dirname = path.resolve();

function build() {
  console.log('Cleaning dist directory...');
  if (fs.existsSync(path.join(__dirname, 'dist'))) {
    fs.rmSync(path.join(__dirname, 'dist'), { recursive: true, force: true });
  }
  fs.mkdirSync(path.join(__dirname, 'dist'), { recursive: true });


  console.log('Copying v2 landing page...');
  fs.copyFileSync(
    path.join(__dirname, 'index-v2.html'),
    path.join(__dirname, 'dist/index.html')
  );

  console.log('Copying static assets to root dist...');
  const publicDir = path.join(__dirname, 'public');
  const distDir = path.join(__dirname, 'dist');
  
  if (fs.existsSync(publicDir)) {
    fs.cpSync(publicDir, distDir, { recursive: true });
  }

  console.log('Build completed successfully!');
}

try {
  build();
} catch (err) {
  console.error('Build failed:', err);
  process.exit(1);
}
