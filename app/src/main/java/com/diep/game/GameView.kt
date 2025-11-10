package com.diep.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.random.Random

@SuppressLint("ViewConstructor")
class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private val tank: Tank = Tank(0f, 0f)
    private val bullets = mutableListOf<Bullet>()
    private val enemies = mutableListOf<Enemy>()

    private val worldSize = 3000f
    private var cameraX = 0f
    private var cameraY = 0f

    private var joystickTouchId = -1
    private var joystickBaseX = 150f
    private var joystickBaseY = 0f
    private var joystickX = 0f
    private var joystickY = 0f
    private val joystickRadius = 80f
    private val joystickMaxDistance = 60f

    private var aimTouchId = -1
    private var lastAimX = 0f
    private var lastAimY = 0f

    private var shootTimer = 0f
    private val shootCooldown = 0.15f // shots per second

    private var gameOver = false
    private var enemySpawnTimer = 0f

    private val backgroundPaint = Paint().apply {
        color = 0xFFCDCDCD.toInt()
        style = Paint.Style.FILL
    }

    private val gridPaint = Paint().apply {
        color = 0xFFB8B8B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val joystickBasePaint = Paint().apply {
        color = 0x88000000.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val joystickStickPaint = Paint().apply {
        color = 0xCC000000.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = 0xFF000000.toInt()
        textSize = 40f
        isAntiAlias = true
    }

    private val gameOverPaint = Paint().apply {
        color = 0xFFFF0000.toInt()
        textSize = 80f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
        isFocusable = true

        // Spawn initial enemies
        spawnEnemies(20)
    }

    private fun spawnEnemies(count: Int) {
        repeat(count) {
            val type = when (Random.nextInt(100)) {
                in 0..59 -> EnemyType.SQUARE     // 60%
                in 60..84 -> EnemyType.TRIANGLE   // 25%
                else -> EnemyType.PENTAGON        // 15%
            }

            val x = Random.nextFloat() * worldSize - worldSize / 2
            val y = Random.nextFloat() * worldSize - worldSize / 2

            // Don't spawn too close to player
            if (sqrt(x * x + y * y) > 400f) {
                enemies.add(Enemy(x, y, type))
            }
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        gameThread = GameThread(holder, this)
        gameThread?.running = true
        gameThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        joystickBaseY = height - 150f
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameThread?.running = false
        while (retry) {
            try {
                gameThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)

                if (gameOver) {
                    restartGame()
                    return true
                }

                // Check if touching joystick area
                if (x < width / 2 && joystickTouchId == -1) {
                    joystickTouchId = pointerId
                    joystickX = x
                    joystickY = y
                } else if (aimTouchId == -1) {
                    aimTouchId = pointerId
                    lastAimX = x
                    lastAimY = y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pointerId = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)

                    if (pointerId == joystickTouchId) {
                        joystickX = x
                        joystickY = y
                    } else if (pointerId == aimTouchId) {
                        lastAimX = x
                        lastAimY = y
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex
                val pointerId = event.getPointerId(pointerIndex)

                if (pointerId == joystickTouchId) {
                    joystickTouchId = -1
                    joystickX = joystickBaseX
                    joystickY = joystickBaseY
                } else if (pointerId == aimTouchId) {
                    aimTouchId = -1
                }
            }
        }
        return true
    }

    fun update(deltaTime: Float) {
        if (gameOver) return

        // Update joystick and tank movement
        if (joystickTouchId != -1) {
            val dx = joystickX - joystickBaseX
            val dy = joystickY - joystickBaseY
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > joystickMaxDistance) {
                joystickX = joystickBaseX + (dx / distance) * joystickMaxDistance
                joystickY = joystickBaseY + (dy / distance) * joystickMaxDistance
            }

            val moveX = (joystickX - joystickBaseX) / joystickMaxDistance
            val moveY = (joystickY - joystickBaseY) / joystickMaxDistance

            tank.velocityX = moveX * 300f
            tank.velocityY = moveY * 300f
        } else {
            tank.velocityX = 0f
            tank.velocityY = 0f
        }

        // Aim tank
        if (aimTouchId != -1) {
            val worldAimX = lastAimX + cameraX - width / 2
            val worldAimY = lastAimY + cameraY - height / 2
            tank.aimAt(worldAimX, worldAimY)
        }

        // Auto-shoot
        shootTimer -= deltaTime
        if (aimTouchId != -1 && shootTimer <= 0) {
            bullets.add(tank.shoot())
            shootTimer = shootCooldown
        }

        // Update tank
        tank.update(deltaTime)

        // Constrain tank to world
        tank.x = tank.x.coerceIn(-worldSize / 2, worldSize / 2)
        tank.y = tank.y.coerceIn(-worldSize / 2, worldSize / 2)

        // Update camera
        cameraX = tank.x - width / 2
        cameraY = tank.y - height / 2

        // Update bullets
        bullets.forEach { it.update(deltaTime) }
        bullets.removeAll { !it.isAlive }

        // Update enemies
        enemies.forEach { it.update(deltaTime) }

        // Check bullet-enemy collisions
        val bulletsToRemove = mutableListOf<Bullet>()
        bullets.forEach { bullet ->
            if (bullet.isPlayerBullet) {
                enemies.forEach { enemy ->
                    if (enemy.isAlive && bullet.collidesWith(enemy)) {
                        val score = enemy.takeDamage(bullet.damage)
                        tank.addScore(score)
                        bullet.isAlive = false
                        bulletsToRemove.add(bullet)
                    }
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
        enemies.removeAll { !it.isAlive }

        // Check tank-enemy collisions
        enemies.forEach { enemy ->
            if (tank.collidesWith(enemy)) {
                tank.takeDamage(20f * deltaTime)
            }
        }

        // Spawn new enemies
        enemySpawnTimer -= deltaTime
        if (enemySpawnTimer <= 0 && enemies.size < 50) {
            spawnEnemies(5)
            enemySpawnTimer = 3f
        }

        // Check game over
        if (!tank.isAlive) {
            gameOver = true
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        // Draw background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        // Draw grid
        val gridSize = 50f
        val startX = ((cameraX / gridSize).toInt() * gridSize)
        val startY = ((cameraY / gridSize).toInt() * gridSize)

        for (i in 0..width / gridSize.toInt() + 1) {
            val x = startX + i * gridSize - cameraX
            canvas.drawLine(x, 0f, x, height.toFloat(), gridPaint)
        }
        for (i in 0..height / gridSize.toInt() + 1) {
            val y = startY + i * gridSize - cameraY
            canvas.drawLine(0f, y, width.toFloat(), y, gridPaint)
        }

        // Draw enemies
        enemies.forEach { it.draw(canvas, cameraX, cameraY) }

        // Draw bullets
        bullets.forEach { it.draw(canvas, cameraX, cameraY) }

        // Draw tank
        tank.draw(canvas, cameraX, cameraY)

        // Draw UI
        drawUI(canvas)

        // Draw game over
        if (gameOver) {
            canvas.drawText("GAME OVER!", width / 2f, height / 2f - 50, gameOverPaint)
            canvas.drawText("Tap to Restart", width / 2f, height / 2f + 50, gameOverPaint.apply {
                textSize = 40f
            })
            gameOverPaint.textSize = 80f
        }
    }

    private fun drawUI(canvas: Canvas) {
        // Draw joystick
        if (joystickTouchId != -1) {
            canvas.drawCircle(joystickBaseX, joystickBaseY, joystickRadius, joystickBasePaint)
            canvas.drawCircle(joystickX, joystickY, joystickRadius / 2, joystickStickPaint)
        } else {
            canvas.drawCircle(joystickBaseX, joystickBaseY, joystickRadius, joystickBasePaint.apply {
                alpha = 128
            })
            canvas.drawCircle(joystickBaseX, joystickBaseY, joystickRadius / 2, joystickStickPaint.apply {
                alpha = 128
            })
            joystickBasePaint.alpha = 136
            joystickStickPaint.alpha = 204
        }

        // Draw score and level
        canvas.drawText("Score: ${tank.score}", 20f, 50f, textPaint)
        canvas.drawText("Level: ${tank.level}", 20f, 100f, textPaint)
    }

    private fun restartGame() {
        tank.x = 0f
        tank.y = 0f
        tank.health = 100f
        tank.maxHealth = 100f
        tank.score = 0
        tank.level = 1
        tank.isAlive = true

        bullets.clear()
        enemies.clear()
        spawnEnemies(20)

        gameOver = false
        shootTimer = 0f
        enemySpawnTimer = 3f
    }

    inner class GameThread(
        private val surfaceHolder: SurfaceHolder,
        private val gameView: GameView
    ) : Thread() {

        var running = false
        private val targetFPS = 60
        private val targetTime = 1000 / targetFPS

        override fun run() {
            var lastTime = System.currentTimeMillis()

            while (running) {
                val currentTime = System.currentTimeMillis()
                val deltaTime = (currentTime - lastTime) / 1000f
                lastTime = currentTime

                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        synchronized(surfaceHolder) {
                            gameView.update(deltaTime)
                            gameView.draw(canvas)
                        }
                    }
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    }
                }

                val elapsed = System.currentTimeMillis() - currentTime
                if (elapsed < targetTime) {
                    try {
                        sleep(targetTime - elapsed)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
