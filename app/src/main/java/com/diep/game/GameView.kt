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
    private val bots = mutableListOf<Bot>()

    private val worldSize = 3000f
    private var cameraX = 0f
    private var cameraY = 0f

    // Left joystick for movement
    private var joystickTouchId = -1
    private var joystickBaseX = 150f
    private var joystickBaseY = 0f
    private var joystickX = 0f
    private var joystickY = 0f
    private val joystickRadius = 80f
    private val joystickMaxDistance = 60f

    // Right joystick for aiming
    private var aimJoystickTouchId = -1
    private var aimJoystickBaseX = 0f
    private var aimJoystickBaseY = 0f
    private var aimJoystickX = 0f
    private var aimJoystickY = 0f
    private val aimJoystickRadius = 80f
    private val aimJoystickMaxDistance = 60f

    private var shootTimer = 0f
    private val shootCooldown = 0.15f // shots per second

    private var gameOver = false
    private var enemySpawnTimer = 0f
    private var showStatUpgradeMenu = false
    private var showClassUpgradeMenu = false

    // UI rectangles for stat upgrades (will be calculated in drawStatUpgradeMenu)
    private val statButtonRects = mutableMapOf<StatType, android.graphics.RectF>()
    private val classButtonRects = mutableMapOf<TankClass, android.graphics.RectF>()

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
        color = 0x88555555.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val joystickStickPaint = Paint().apply {
        color = 0xCC888888.toInt()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val aimJoystickBasePaint = Paint().apply {
        color = 0x88FC7677.toInt()  // Red tint for aim joystick
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val aimJoystickStickPaint = Paint().apply {
        color = 0xCCFF0000.toInt()  // Bright red for aim stick
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

    private val overlayPaint = Paint().apply {
        color = 0xCC000000.toInt() // Semi-transparent black
        style = Paint.Style.FILL
    }

    private val buttonPaint = Paint().apply {
        color = 0xFF4CAF50.toInt() // Green
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val buttonDisabledPaint = Paint().apply {
        color = 0xFF757575.toInt() // Gray
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val buttonTextPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 30f
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
    }

    private val titlePaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
        textSize = 50f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
        isFocusable = true

        // Spawn initial enemies and bots
        spawnEnemies(20)
        spawnBots(3)
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

    private fun spawnBots(count: Int) {
        repeat(count) {
            val x = Random.nextFloat() * worldSize - worldSize / 2
            val y = Random.nextFloat() * worldSize - worldSize / 2

            // Don't spawn too close to player
            if (sqrt(x * x + y * y) > 500f) {
                bots.add(Bot(x, y))
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
        aimJoystickBaseX = width - 150f
        aimJoystickBaseY = height - 150f
        aimJoystickX = aimJoystickBaseX
        aimJoystickY = aimJoystickBaseY
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

                // Handle stat upgrade menu clicks
                if (showStatUpgradeMenu) {
                    statButtonRects.forEach { (stat, rect) ->
                        if (rect.contains(x, y)) {
                            if (tank.upgradeStat(stat)) {
                                // Successfully upgraded
                                if (tank.availableStatPoints == 0) {
                                    showStatUpgradeMenu = false
                                }
                            }
                            return true
                        }
                    }
                    return true // Consume all touches when menu is open
                }

                // Handle tank class upgrade menu clicks
                if (showClassUpgradeMenu) {
                    classButtonRects.forEach { (tankClass, rect) ->
                        if (rect.contains(x, y)) {
                            if (tank.upgradeTankClass(tankClass)) {
                                showClassUpgradeMenu = false
                            }
                            return true
                        }
                    }
                    return true // Consume all touches when menu is open
                }

                // Check if touching left side (movement joystick)
                if (x < width / 2 && joystickTouchId == -1) {
                    joystickTouchId = pointerId
                    joystickX = x
                    joystickY = y
                }
                // Check if touching right side (aim joystick)
                else if (x >= width / 2 && aimJoystickTouchId == -1) {
                    aimJoystickTouchId = pointerId
                    aimJoystickX = x
                    aimJoystickY = y
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
                    } else if (pointerId == aimJoystickTouchId) {
                        aimJoystickX = x
                        aimJoystickY = y
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
                } else if (pointerId == aimJoystickTouchId) {
                    aimJoystickTouchId = -1
                    aimJoystickX = aimJoystickBaseX
                    aimJoystickY = aimJoystickBaseY
                }
            }
        }
        return true
    }

    fun update(deltaTime: Float) {
        if (gameOver) return

        // Open stat upgrade menu if tank has available stat points
        if (tank.availableStatPoints > 0 && !showStatUpgradeMenu) {
            showStatUpgradeMenu = true
        }

        // Pause gameplay when menus are open
        if (showStatUpgradeMenu || showClassUpgradeMenu) {
            return
        }

        // Update movement joystick and tank movement
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

            val moveSpeed = tank.getMoveSpeed()
            tank.velocityX = moveX * moveSpeed
            tank.velocityY = moveY * moveSpeed
        } else {
            tank.velocityX = 0f
            tank.velocityY = 0f
        }

        // Update aim joystick and tank aiming
        if (aimJoystickTouchId != -1) {
            val dx = aimJoystickX - aimJoystickBaseX
            val dy = aimJoystickY - aimJoystickBaseY
            val distance = sqrt(dx * dx + dy * dy)

            if (distance > aimJoystickMaxDistance) {
                aimJoystickX = aimJoystickBaseX + (dx / distance) * aimJoystickMaxDistance
                aimJoystickY = aimJoystickBaseY + (dy / distance) * aimJoystickMaxDistance
            }

            // Only aim if joystick is moved significantly
            if (distance > 10f) {
                val aimAngle = atan2(dy, dx)
                tank.angle = aimAngle
            }

            // Auto-shoot when aim joystick is active
            shootTimer -= deltaTime
            if (distance > 10f && shootTimer <= 0) {
                bullets.addAll(tank.shoot())
                shootTimer = tank.getShootCooldown()
            }
        }
        // Note: Tank angle is now controlled ONLY by the right joystick
        // No auto-aiming when moving without aiming

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

        // Update bots
        bots.forEach { bot ->
            bot.updateAI(deltaTime, tank, enemies, bots.filter { it != bot })
            bot.update(deltaTime)

            // Constrain bot to world
            bot.x = bot.x.coerceIn(-worldSize / 2, worldSize / 2)
            bot.y = bot.y.coerceIn(-worldSize / 2, worldSize / 2)

            // Bot shooting
            if (bot.canShoot()) {
                bullets.add(bot.shoot())
                bot.shootTimer = bot.shootCooldown // Reset shoot timer after firing
            }
        }

        // Check bullet collisions
        val bulletsToRemove = mutableListOf<Bullet>()
        bullets.forEach { bullet ->
            // Player bullets hit enemies and bots
            if (bullet.isPlayerBullet) {
                // Hit enemies
                enemies.forEach { enemy ->
                    if (enemy.isAlive && bullet.collidesWith(enemy)) {
                        val score = enemy.takeDamage(bullet.damage)
                        tank.addScore(score)
                        bullet.isAlive = false
                        bulletsToRemove.add(bullet)
                    }
                }
                // Hit bots
                bots.forEach { bot ->
                    if (bot.isAlive && bullet.collidesWith(bot)) {
                        bot.takeDamage(bullet.damage)
                        if (!bot.isAlive) {
                            tank.addScore(50) // Score for killing a bot
                        }
                        bullet.isAlive = false
                        bulletsToRemove.add(bullet)
                    }
                }
            }
            // Bot bullets hit player, enemies, and other bots
            else {
                // Hit player
                if (tank.isAlive && bullet.collidesWith(tank)) {
                    tank.takeDamage(bullet.damage)
                    bullet.isAlive = false
                    bulletsToRemove.add(bullet)
                }
                // Hit enemies
                enemies.forEach { enemy ->
                    if (enemy.isAlive && bullet.collidesWith(enemy)) {
                        val score = enemy.takeDamage(bullet.damage)
                        // Find which bot fired this bullet and give them score
                        bots.forEach { bot ->
                            if (bot.isAlive && score > 0) {
                                bot.addScore(score)
                            }
                        }
                        bullet.isAlive = false
                        bulletsToRemove.add(bullet)
                    }
                }
                // Hit other bots
                bots.forEach { bot ->
                    if (bot.isAlive && bullet.collidesWith(bot)) {
                        bot.takeDamage(bullet.damage)
                        bullet.isAlive = false
                        bulletsToRemove.add(bullet)
                    }
                }
            }
        }
        bullets.removeAll(bulletsToRemove)
        enemies.removeAll { !it.isAlive }
        bots.removeAll { !it.isAlive }

        // Check tank-enemy collisions
        enemies.forEach { enemy ->
            if (tank.collidesWith(enemy)) {
                tank.takeDamage(20f * deltaTime)
            }
        }

        // Check tank-bot collisions
        bots.forEach { bot ->
            if (tank.collidesWith(bot)) {
                tank.takeDamage(15f * deltaTime)
                bot.takeDamage(15f * deltaTime)
            }
        }

        // Check bot-enemy collisions
        bots.forEach { bot ->
            enemies.forEach { enemy ->
                if (bot.collidesWith(enemy)) {
                    bot.takeDamage(15f * deltaTime)
                }
            }
        }

        // Spawn new enemies
        enemySpawnTimer -= deltaTime
        if (enemySpawnTimer <= 0 && enemies.size < 50) {
            spawnEnemies(5)
            enemySpawnTimer = 3f
        }

        // Spawn new bots if too few
        if (bots.size < 3) {
            spawnBots(1)
        }

        // Check for class upgrade availability
        val availableUpgrades = tank.tankClass.getNextUpgrade(tank.level)
        showClassUpgradeMenu = availableUpgrades.isNotEmpty()

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

        // Draw bots
        bots.forEach { it.draw(canvas, cameraX, cameraY) }

        // Draw bullets
        bullets.forEach { it.draw(canvas, cameraX, cameraY) }

        // Draw tank
        tank.draw(canvas, cameraX, cameraY)

        // Draw UI
        drawUI(canvas)

        // Draw upgrade menus (on top of everything)
        if (showStatUpgradeMenu) {
            drawStatUpgradeMenu(canvas)
        } else if (showClassUpgradeMenu) {
            drawClassUpgradeMenu(canvas)
        }

        // Draw game over
        if (gameOver) {
            canvas.drawText("GAME OVER!", width / 2f, height / 2f - 50, gameOverPaint)
            canvas.drawText("Tap to Restart", width / 2f, height / 2f + 50, gameOverPaint.apply {
                textSize = 40f
            })
            gameOverPaint.textSize = 80f
        }
    }

    private fun drawStatUpgradeMenu(canvas: Canvas) {
        // Draw overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Draw title
        canvas.drawText("UPGRADE STATS", width / 2f, 100f, titlePaint)
        canvas.drawText("Points: ${tank.availableStatPoints}", width / 2f, 160f, titlePaint.apply {
            textSize = 40f
        })
        titlePaint.textSize = 50f

        // Clear previous button rects
        statButtonRects.clear()

        // Draw stat buttons
        val startY = 220f
        val buttonWidth = width - 100f
        val buttonHeight = 80f
        val spacing = 10f

        val stats = StatType.values()
        stats.forEachIndexed { index, stat ->
            val y = startY + index * (buttonHeight + spacing)
            val rect = android.graphics.RectF(50f, y, 50f + buttonWidth, y + buttonHeight)
            statButtonRects[stat] = rect

            // Choose paint based on whether stat can be upgraded
            val paint = if (tank.stats.canUpgrade(stat)) buttonPaint else buttonDisabledPaint
            canvas.drawRect(rect, paint)

            // Draw stat name and level
            val level = tank.stats.getStatLevel(stat)
            val statText = "${stat.displayName}: $level / ${TankStats.MAX_STAT_LEVEL}"
            canvas.drawText(statText, 70f, y + 50f, buttonTextPaint)

            // Draw stat effect
            val effectText = when (stat) {
                StatType.HEALTH_REGEN -> "${tank.stats.getHealthRegenRate()} HP/s"
                StatType.MAX_HEALTH -> "+${tank.stats.getMaxHealthBonus().toInt()} HP"
                StatType.BODY_DAMAGE -> "${(tank.stats.getBodyDamageMultiplier() * 100).toInt()}%"
                StatType.BULLET_SPEED -> "${(tank.stats.getBulletSpeedMultiplier() * 100).toInt()}%"
                StatType.BULLET_PENETRATION -> "+${tank.stats.getBulletPenetrationBonus().toInt()}"
                StatType.BULLET_DAMAGE -> "${(tank.stats.getBulletDamageMultiplier() * 100).toInt()}%"
                StatType.RELOAD -> "${(100 / tank.stats.getReloadMultiplier()).toInt()}%"
                StatType.MOVEMENT_SPEED -> "${(tank.stats.getMovementSpeedMultiplier() * 100).toInt()}%"
            }
            buttonTextPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(effectText, width - 70f, y + 50f, buttonTextPaint)
            buttonTextPaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawClassUpgradeMenu(canvas: Canvas) {
        // Draw overlay
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        // Draw title
        canvas.drawText("LEVEL 15 UPGRADE!", width / 2f, 100f, titlePaint)
        canvas.drawText("Choose Your Class", width / 2f, 160f, titlePaint.apply {
            textSize = 40f
        })
        titlePaint.textSize = 50f

        // Clear previous button rects
        classButtonRects.clear()

        // Get available upgrades
        val availableUpgrades = tank.tankClass.getNextUpgrade(tank.level)

        // Draw class buttons
        val startY = 220f
        val buttonWidth = width - 100f
        val buttonHeight = 100f
        val spacing = 15f

        availableUpgrades.forEachIndexed { index, tankClass ->
            val y = startY + index * (buttonHeight + spacing)
            val rect = android.graphics.RectF(50f, y, 50f + buttonWidth, y + buttonHeight)
            classButtonRects[tankClass] = rect

            canvas.drawRect(rect, buttonPaint)

            // Draw class name
            canvas.drawText(tankClass.displayName, 70f, y + 40f, buttonTextPaint.apply {
                textSize = 40f
            })

            // Draw class stats
            val statsText = "Fire Rate: ${(tankClass.fireRate * 100).toInt()}% | " +
                    "Damage: ${(tankClass.bulletDamage * 100).toInt()}% | " +
                    "Speed: ${(tankClass.bulletSpeed * 100).toInt()}%"
            canvas.drawText(statsText, 70f, y + 75f, buttonTextPaint.apply {
                textSize = 25f
            })
            buttonTextPaint.textSize = 30f
        }

        // Draw instruction
        titlePaint.textSize = 30f
        canvas.drawText("Tap to select a class", width / 2f, height - 50f, titlePaint)
        titlePaint.textSize = 50f
    }

    private fun drawUI(canvas: Canvas) {
        // Draw left movement joystick
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

        // Draw right aim joystick
        if (aimJoystickTouchId != -1) {
            canvas.drawCircle(aimJoystickBaseX, aimJoystickBaseY, aimJoystickRadius, aimJoystickBasePaint)
            canvas.drawCircle(aimJoystickX, aimJoystickY, aimJoystickRadius / 2, aimJoystickStickPaint)
        } else {
            canvas.drawCircle(aimJoystickBaseX, aimJoystickBaseY, aimJoystickRadius, aimJoystickBasePaint.apply {
                alpha = 80
            })
            canvas.drawCircle(aimJoystickBaseX, aimJoystickBaseY, aimJoystickRadius / 2, aimJoystickStickPaint.apply {
                alpha = 100
            })
            aimJoystickBasePaint.alpha = 136
            aimJoystickStickPaint.alpha = 204
        }

        // Draw score, level, and tank class
        canvas.drawText("Score: ${tank.score}", 20f, 50f, textPaint)
        canvas.drawText("Level: ${tank.level}", 20f, 100f, textPaint)
        canvas.drawText("Class: ${tank.tankClass.displayName}", 20f, 150f, textPaint)

        // Draw stat points indicator (menu will auto-open)
        if (tank.availableStatPoints > 0 && !showStatUpgradeMenu) {
            textPaint.color = 0xFFFFD700.toInt() // Gold color
            canvas.drawText("Upgrade Points: ${tank.availableStatPoints}", width - 300f, 50f, textPaint)
            textPaint.color = 0xFF000000.toInt()
        }
    }

    private fun restartGame() {
        tank.x = 0f
        tank.y = 0f
        tank.health = 100f
        tank.maxHealth = 100f
        tank.score = 0
        tank.level = 1
        tank.availableStatPoints = 0
        tank.isAlive = true
        tank.tankClass = TankClass.BASIC
        // Reset all stats
        tank.stats.healthRegen = 0
        tank.stats.maxHealth = 0
        tank.stats.bodyDamage = 0
        tank.stats.bulletSpeed = 0
        tank.stats.bulletPenetration = 0
        tank.stats.bulletDamage = 0
        tank.stats.reload = 0
        tank.stats.movementSpeed = 0

        bullets.clear()
        enemies.clear()
        bots.clear()
        spawnEnemies(20)
        spawnBots(3)

        // Reset joysticks
        joystickTouchId = -1
        aimJoystickTouchId = -1
        joystickX = joystickBaseX
        joystickY = joystickBaseY
        aimJoystickX = aimJoystickBaseX
        aimJoystickY = aimJoystickBaseY

        gameOver = false
        shootTimer = 0f
        enemySpawnTimer = 3f
        showStatUpgradeMenu = false
        showClassUpgradeMenu = false
        statButtonRects.clear()
        classButtonRects.clear()
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
