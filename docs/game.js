"use strict";

const canvas = document.querySelector("#game-canvas");
const context = canvas.getContext("2d");
const startScreen = document.querySelector("#start-screen");
const gameOverScreen = document.querySelector("#game-over-screen");
const startButton = document.querySelector("#start-button");
const restartButton = document.querySelector("#restart-button");
const scoreLabel = document.querySelector("#score");
const finalScore = document.querySelector("#final-score");

const directions = [
  [1, -1], [-1, -1], [1, 1], [-1, 1],
  [1, 0], [-1, 0], [0, 1], [0, -1],
];

const lineLength = 25;
const hitDistance = 20;
let screen = "start";
let slats = [];
let score = 0;
let lastFrameTime = 0;
let animationFrame = 0;

function resizeCanvas() {
  const ratio = Math.min(window.devicePixelRatio || 1, 3);
  const rect = canvas.getBoundingClientRect();
  canvas.width = Math.round(rect.width * ratio);
  canvas.height = Math.round(rect.height * ratio);
  context.setTransform(ratio, 0, 0, ratio, 0, 0);
  draw();
}

function startGame() {
  score = 0;
  slats = [{
    x: canvas.clientWidth / 2,
    y: canvas.clientHeight / 2,
    size: 0,
    growthSpeed: 60,
  }];
  lastFrameTime = 0;
  screen = "game";
  updateInterface();
  cancelAnimationFrame(animationFrame);
  animationFrame = requestAnimationFrame(gameLoop);
}

function gameLoop(time) {
  if (screen !== "game") return;

  const elapsed = lastFrameTime === 0
    ? 1 / 60
    : Math.min((time - lastFrameTime) / 1000, 0.05);
  lastFrameTime = time;

  for (const slat of slats) {
    slat.size += slat.growthSpeed * elapsed;
  }

  slats = slats.filter((slat) => !slatIsFullyOffScreen(slat));
  if (slats.length === 0) {
    screen = "gameOver";
    updateInterface();
    draw();
    return;
  }

  draw();
  animationFrame = requestAnimationFrame(gameLoop);
}

function draw() {
  context.clearRect(0, 0, canvas.clientWidth, canvas.clientHeight);
  context.fillStyle = "#000";
  context.fillRect(0, 0, canvas.clientWidth, canvas.clientHeight);

  context.strokeStyle = "#fff";
  context.lineWidth = 2;
  context.lineCap = "round";

  for (const slat of slats) {
    for (const [dx, dy] of directions) {
      context.beginPath();
      context.moveTo(slat.x + dx * slat.size, slat.y + dy * slat.size);
      context.lineTo(
        slat.x + dx * (slat.size + lineLength),
        slat.y + dy * (slat.size + lineLength),
      );
      context.stroke();
    }
  }
}

function handlePointer(event) {
  if (screen !== "game") return;
  event.preventDefault();

  const rect = canvas.getBoundingClientRect();
  const point = { x: event.clientX - rect.left, y: event.clientY - rect.top };
  const snapshot = [...slats];

  for (const slat of snapshot) {
    if (!touchIsOnWhitePart(point, slat)) continue;

    slats.push({
      x: point.x,
      y: point.y,
      size: 0,
      growthSpeed: currentGrowthSpeed(),
    });
    score += 1;
    scoreLabel.textContent = `Score: ${score}`;
    return;
  }
}

function currentGrowthSpeed() {
  const easing = 1 - Math.exp(-score * 0.03);
  return (1.9 + 1.2 * easing) * 60;
}

function touchIsOnWhitePart(point, slat) {
  return directions.some(([dx, dy]) => {
    const start = {
      x: slat.x + dx * slat.size,
      y: slat.y + dy * slat.size,
    };
    const end = {
      x: slat.x + dx * (slat.size + lineLength),
      y: slat.y + dy * (slat.size + lineLength),
    };
    return pointNearLine(point, start, end);
  });
}

function pointNearLine(point, start, end) {
  const lineX = end.x - start.x;
  const lineY = end.y - start.y;
  const lengthSquared = lineX * lineX + lineY * lineY;
  if (lengthSquared === 0) return false;

  const projection = (
    (point.x - start.x) * lineX + (point.y - start.y) * lineY
  ) / lengthSquared;
  const parameter = Math.max(0, Math.min(1, projection));
  const closestX = start.x + parameter * lineX;
  const closestY = start.y + parameter * lineY;
  return Math.hypot(point.x - closestX, point.y - closestY) < hitDistance;
}

function slatIsFullyOffScreen(slat) {
  const width = canvas.clientWidth;
  const height = canvas.clientHeight;

  return directions.every(([dx, dy]) => {
    const startX = slat.x + dx * slat.size;
    const startY = slat.y + dy * slat.size;
    const endX = slat.x + dx * (slat.size + lineLength);
    const endY = slat.y + dy * (slat.size + lineLength);
    return !pointIsOnScreen(startX, startY, width, height)
      && !pointIsOnScreen(endX, endY, width, height);
  });
}

function pointIsOnScreen(x, y, width, height) {
  return x >= 0 && x <= width && y >= 0 && y <= height;
}

function updateInterface() {
  startScreen.hidden = screen !== "start";
  gameOverScreen.hidden = screen !== "gameOver";
  scoreLabel.hidden = screen !== "game";
  scoreLabel.textContent = `Score: ${score}`;
  finalScore.textContent = `Score: ${score}`;
}

startButton.addEventListener("click", startGame);
restartButton.addEventListener("click", startGame);
canvas.addEventListener("pointerdown", handlePointer);
window.addEventListener("resize", resizeCanvas);
document.addEventListener("visibilitychange", () => {
  if (document.hidden) lastFrameTime = 0;
});

resizeCanvas();
updateInterface();
