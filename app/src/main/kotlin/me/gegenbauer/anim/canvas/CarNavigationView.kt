package me.gegenbauer.anim.canvas

import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.animation.PathInterpolatorCompat
import me.gegenbauer.anim.R
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.cos
import kotlin.math.sin

class CarNavigationView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs), NavigationStateful {

    override var state: NavigationState? = null
        set(value) {
            if (field != value) {
                field?.onExitState()
                field = value
                field?.initParams()
                field?.onEnterState()
            }
        }

    internal val dimens = Dimens(context)

    internal val centerPoint = CenterPoint(context, dimens)
    internal val frontSector = FrontSector(context, dimens)
    internal val emptyProgressBar = EmptyProgressBar(context, dimens)
    internal val progressBar = ProgressBar(context, dimens)
    internal val carArrow = CarArrow(context, dimens)
    internal val iconCar = IconCar(context, dimens)
    internal val iconCarBackground = IconCarBackground(context, dimens)
    internal val carWaterWave = CarWaterWave(context, dimens, this)

    private val uiParts = listOf(
        emptyProgressBar,
        progressBar,
        frontSector,
        centerPoint,
        carArrow,
        carWaterWave,
        iconCarBackground,
        iconCar,
    )

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        uiParts.forEach { it.draw(canvas) }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = width + dimens.carIconSize
        setMeasuredDimension(width, height)
        dimens.viewSize.set(width, height)
        uiParts.forEach { it.onDimensChanged() }
    }
}

internal class Dimens(context: Context) {
    val carIconSize = context.resources.getDimensionPixelSize(R.dimen.car_icon_size)
    val carIconBgRadius = context.resources.getDimensionPixelSize(R.dimen.car_bg_radius)
    val centerPointRadius = context.resources.getDimensionPixelSize(R.dimen.center_point_radius)
    val progressBarRadius = context.resources.getDimensionPixelSize(R.dimen.progress_bar_radius)
    val progressBarWidth = context.resources.getDimensionPixelSize(R.dimen.progress_bar_width)
    val arrowDistance = context.resources.getDimensionPixelSize(R.dimen.arrow_distance)

    val viewSize: Size = Size(0, 0)
}

interface NavigationState {

    val naviView: CarNavigationView

    var progress: Float

    fun initParams()

    fun onProgressChanged()

    fun display()

    fun onEnterState()

    fun onExitState()
}

internal abstract class BaseState(override val naviView: CarNavigationView) : NavigationState {

    override var progress: Float = 0f
        set(value) {
            if (field != value) {
                field = value
                onProgressChanged()
            }
            field = value
        }

    protected val dimens: Dimens
        get() = naviView.dimens

    override fun onProgressChanged() {
        // do nothing
    }

    override fun display() {
        naviView.invalidate()
    }

    override fun onEnterState() {

    }

}

internal class SearchState(naviView: CarNavigationView) : BaseState(naviView) {

    override fun initParams() {
        naviView.centerPoint.params = UIParams(
            PolarPosition(0f, 0f)
        )
        naviView.frontSector.params = UIParams(
            PolarPosition(0f, 0f)
        )
        naviView.emptyProgressBar.params = UIParams(
            PolarPosition(0f, 0f),
            alpha = 0.1f
        )
        naviView.iconCar.params = UIParams(
            PolarPosition(dimens.progressBarRadius.toFloat(), 0f)
        )
        naviView.iconCarBackground.params = UIParams(
            PolarPosition(dimens.progressBarRadius.toFloat(), 0f)
        )
        naviView.carWaterWave.params = UIParams(
            PolarPosition(dimens.progressBarRadius.toFloat(), 0f)
        )
        naviView.progressBar.params = UIParams(
            PolarPosition(0f, -90f),
        )
        naviView.carArrow.params = UIParams(
            PolarPosition(dimens.arrowDistance.toFloat(), 270f),
        )
    }

    override fun onProgressChanged() {
        naviView.iconCar.setParams(
            polarPosition = PolarPosition(dimens.progressBarRadius.toFloat(), 270 - progress * 360f)
        )
        naviView.iconCarBackground.setParams(
            polarPosition = PolarPosition(dimens.progressBarRadius.toFloat(), 270 - progress * 360f)
        )
        naviView.carWaterWave.setParams(
            polarPosition = PolarPosition(dimens.progressBarRadius.toFloat(), 270 - progress * 360f)
        )
        naviView.progressBar.setParams(
            polarPosition = PolarPosition(dimens.progressBarRadius.toFloat(), 270 - progress * 360f)
        )
        if (progress == 1f) {
            startAtFrontAnim()
        } else {
            stopAtFrontAnim()
            naviView.invalidate()
        }
    }

    override fun onExitState() {
        naviView.carWaterWave.stop()
        naviView.carArrow.stopAnim()
    }

    private fun startAtFrontAnim() {
        naviView.carWaterWave.start(0.2f, 1.6f)
        naviView.carArrow.startFadeAnim(false)
    }

    private fun stopAtFrontAnim() {
        naviView.carWaterWave.stop()
        naviView.carArrow.stopAnim()
    }

}

internal class NearbyState(override val naviView: CarNavigationView) : BaseState(naviView) {

    override var progress: Float = 0f

    private val anims = mutableListOf<AnimatorSet>()

    override fun initParams() {
        naviView.centerPoint.params = UIParams(
            PolarPosition(0f, 0f)
        )
        naviView.frontSector.params = UIParams(
            PolarPosition(0f, 0f)
        )
        naviView.emptyProgressBar.params = UIParams(
            PolarPosition(0f, 0f),
            alpha = 0.1f
        )
        naviView.iconCar.params = UIParams(
            PolarPosition(dimens.progressBarRadius.toFloat(), -90f)
        )
        naviView.iconCarBackground.params = UIParams(
            PolarPosition(dimens.progressBarRadius.toFloat(), -90f)
        )
        naviView.carWaterWave.params = UIParams(
            PolarPosition(dimens.progressBarRadius.toFloat(), -90f),
            alpha = 0f,
            scale = 1f
        )
        naviView.progressBar.params = UIParams(
            PolarPosition(0f, -90f),
        )
        naviView.carArrow.params = UIParams(
            PolarPosition(dimens.arrowDistance.toFloat(), -90f),
            alpha = 0f
        )
    }

    override fun onEnterState() {
        startIntermediateAnim()
    }

    private fun startIntermediateAnim() {
        naviView.carWaterWave.setParams(
            polarPosition = PolarPosition(0f, -90f)
        )
        val translationAnim = ValueAnimator.ofFloat(0f, dimens.progressBarRadius.toFloat()).apply {
            duration = 600
            interpolator = alphaInterpolator
        }
        val fadeOutAnim = ValueAnimator.ofFloat(1f, 0f).apply {
            duration = 600
            interpolator = alphaInterpolator
        }
        val emptyProgressBarEnlargeAnim = ValueAnimator.ofFloat(1f, 1.8f).apply {
            duration = 600
            interpolator = alphaInterpolator
        }
        val carBackgroundEnlargeAnim = ValueAnimator.ofFloat(1f, 3.7f).apply {
            duration = 600
            interpolator = alphaInterpolator
        }

        translationAnim.addUpdateListener {
            val translationY = it.animatedValue as Float
            val newPolarPosition =
                PolarPosition(dimens.progressBarRadius.toFloat() - translationY, -90f)
            naviView.iconCar.setParams(
                polarPosition = newPolarPosition
            )
            naviView.iconCarBackground.setParams(
                polarPosition = newPolarPosition
            )
            naviView.carWaterWave.setParams(
                polarPosition = newPolarPosition
            )
            naviView.invalidate()
        }
        fadeOutAnim.addUpdateListener {
            val alpha = it.animatedValue as Float
            naviView.iconCar.setParams(
                alpha = alpha
            )
            naviView.frontSector.setParams(
                alpha = alpha
            )
            naviView.invalidate()
        }
        emptyProgressBarEnlargeAnim.addUpdateListener {
            val scale = it.animatedValue as Float
            naviView.emptyProgressBar.setParams(
                scale = scale,
            )
        }
        carBackgroundEnlargeAnim.addUpdateListener {
            val scale = it.animatedValue as Float
            naviView.iconCarBackground.setParams(
                scale = scale,
            )
            naviView.carWaterWave.setParams(
                scale = scale,
            )
            naviView.invalidate()
        }
        val animSet = AnimatorSet()
        animSet.doOnStart {
            anims.add(animSet)
        }
        animSet.doOnEnd {
            anims.remove(animSet)
        }
        animSet.playTogether(
            translationAnim,
            fadeOutAnim,
            emptyProgressBarEnlargeAnim,
            carBackgroundEnlargeAnim
        )
        animSet.start()
        startNearbyAnim()
    }

    private fun startNearbyAnim() {
        naviView.carWaterWave.start(1f, 1.92f)
    }

    override fun onExitState() {
        anims.toList().forEach { it.cancel() }
    }

}

internal interface NavigationStateful {
    var state: NavigationState?
}

private val scaleInterpolator = PathInterpolatorCompat.create(0f, 0f, 0.52f, 1f)
private val alphaInterpolator = PathInterpolatorCompat.create(0.33f, 0f, 0.67f, 1f)

private interface UIPart {

    val context: Context

    val paint: Paint

    var params: UIParams

    fun setParams(
        polarPosition: PolarPosition = params.polarPosition,
        rotation: Float = params.rotation,
        scale: Float = params.scale,
        alpha: Float = params.alpha
    ) {
        params.polarPosition = polarPosition
        params.rotation = rotation
        params.scale = scale
        params.alpha = alpha
        onParamsChanged()
    }

    fun onParamsChanged()

    fun draw(canvas: Canvas)

    fun onDimensChanged()
}

internal interface IAnimation {
    fun start(vararg params: Any)
    fun stop()
}

internal open class BaseUIPart(override val context: Context, protected val dimens: Dimens) :
    UIPart {

    override var params: UIParams = UIParams(PolarPosition(0f, 0f))
        set(value) {
            field = value
            onParamsChanged()
        }

    override val paint = Paint()

    protected fun getCartesianPosition(): CartesianPosition {
        return params.polarPosition.toCartesian()
            .offset(dimens.viewSize.width / 2f, dimens.viewSize.height / 2f)
    }

    override fun onParamsChanged() {
        paint.alpha = (params.alpha * 255).toInt()
    }

    override fun draw(canvas: Canvas) {
        // do nothing
    }

    override fun onDimensChanged() {
        // do nothing
    }

}

internal data class UIParams(
    var polarPosition: PolarPosition,
    var rotation: Float = 0f,
    var scale: Float = 1f,
    var alpha: Float = 1f
) {
    fun set(
        polarPosition: PolarPosition = this.polarPosition,
        rotation: Float = this.rotation,
        scale: Float = this.scale,
        alpha: Float = this.alpha
    ) {
        this.polarPosition = polarPosition
        this.rotation = rotation
        this.scale = scale
        this.alpha = alpha
    }
}

internal data class PolarPosition(
    val radius: Float,
    val angle: Float
)

internal data class CartesianPosition(
    val x: Float,
    val y: Float
)

internal data class Size(
    var width: Int,
    var height: Int
) {
    fun set(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
}

/**
 * 极坐标转换为直角坐标
 * angle 是角度，需要先转化为弧度
 */
private fun PolarPosition.toCartesian(): CartesianPosition {
    val x = radius * cos(Math.toRadians(angle.toDouble())).toFloat()
    val y = radius * sin(Math.toRadians(angle.toDouble())).toFloat()
    return CartesianPosition(x, y)
}

private fun CartesianPosition.offset(offsetX: Float, offsetY: Float): CartesianPosition {
    return CartesianPosition(x + offsetX, y + offsetY)
}

internal class CenterPoint(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    override val paint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.car_icon_bounds_color)
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas) {
        val cartesianPosition = getCartesianPosition()
        canvas.drawCircle(
            cartesianPosition.x,
            cartesianPosition.y,
            dimens.centerPointRadius.toFloat(),
            paint
        )
    }

}

internal class FrontSector(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    private val bitmap by lazy {
        ContextCompat.getDrawable(context, R.drawable.front_sector)!!.toBitmap()
    }

    override fun draw(canvas: Canvas) {
        val cartesianPosition =
            getCartesianPosition().offset(-bitmap.width / 2f, -bitmap.height.toFloat())
        canvas.drawBitmap(bitmap, cartesianPosition.x, cartesianPosition.y, paint)
    }

}

internal class EmptyProgressBar(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    override val paint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.progress_unused_color)
        style = Paint.Style.STROKE
        strokeWidth = dimens.progressBarWidth.toFloat()
    }

    /**
     * 绘制圆环
     * 半径为：dimens.progressBarRadius
     * 宽度为：dimens.progressBarWidth
     */
    override fun draw(canvas: Canvas) {
        val cartesianPosition = getCartesianPosition()
        canvas.drawCircle(
            cartesianPosition.x,
            cartesianPosition.y,
            dimens.progressBarRadius.toFloat() * params.scale,
            paint
        )
    }

}

internal class ProgressBar(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    override val paint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.progress_used_color)
        style = Paint.Style.STROKE
        strokeWidth = dimens.progressBarWidth.toFloat()
        strokeCap = Paint.Cap.ROUND
    }
    private val progressRect = RectF()

    override fun onDimensChanged() {
        progressRect.set(
            (dimens.viewSize.width - dimens.progressBarRadius * 2) / 2f,
            (dimens.viewSize.height - dimens.progressBarRadius * 2) / 2f,
            (dimens.viewSize.width + dimens.progressBarRadius * 2) / 2f,
            (dimens.viewSize.height + dimens.progressBarRadius * 2) / 2f
        )
    }

    /**
     * 绘制弧形
     */
    override fun draw(canvas: Canvas) {
        val sweepAngles = transformAngleToSweepAngle(params.polarPosition.angle)
        canvas.drawArc(
            progressRect,
            sweepAngles.first,
            sweepAngles.second,
            false,
            paint
        )
    }

    /**
     * progress=[0, 1]
     * angle = 270 - progress * 360f
     * angle=[270, -90]
     * sweepAngle = angle + 90
     * sweepAngle = [360, 0]
     */
    private fun transformAngleToSweepAngle(angle: Float): Pair<Float, Float> {
        return Pair(-90f, angle + 90) // 360,
    }

}

internal class IconCar(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    private val bitmap by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_navi_car)!!.toBitmap()
    }

    override fun draw(canvas: Canvas) {
        val cartesianPosition =
            getCartesianPosition().offset(-bitmap.width / 2f, -bitmap.height / 2f)
        canvas.drawBitmap(bitmap, cartesianPosition.x, cartesianPosition.y, paint)
    }

}

internal class IconCarBackground(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    override val paint: Paint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.car_icon_bounds_color)
    }

    override fun draw(canvas: Canvas) {
        val cartesianPosition = getCartesianPosition()
        canvas.drawCircle(
            cartesianPosition.x,
            cartesianPosition.y,
            dimens.carIconBgRadius.toFloat() * params.scale,
            paint
        )
    }

}

internal class CarArrow(override val context: Context, dimens: Dimens) :
    BaseUIPart(context, dimens) {

    private val bitmap by lazy {
        ContextCompat.getDrawable(context, R.drawable.ic_arrow)!!.toBitmap()
    }

    private var fadeAnim: ValueAnimator? = null

    override fun draw(canvas: Canvas) {
        val cartesianPosition =
            getCartesianPosition().offset(-bitmap.width / 2f, -bitmap.height / 2f)
        canvas.drawBitmap(
            bitmap,
            cartesianPosition.x,
            cartesianPosition.y,
            paint
        )
    }

    fun startFadeAnim(fadeIn: Boolean) {
        fadeAnim?.cancel()
        fadeAnim = if (fadeIn) {
            ValueAnimator.ofFloat(0f, 1f)
        } else {
            ValueAnimator.ofFloat(1f, 0f)
        }.apply {
            duration = 650
            interpolator = alphaInterpolator
            addUpdateListener {
                val alpha = it.animatedValue as Float
                paint.alpha = (alpha * 255).toInt()
            }
            start()
        }
    }

    fun stopAnim() {
        fadeAnim?.cancel()
    }

}

internal class CarWaterWave(
    override val context: Context,
    dimens: Dimens,
    private val naviView: CarNavigationView
) :
    BaseUIPart(context, dimens), IAnimation {
    override var params: UIParams = UIParams(PolarPosition(0f, 0f))
        set(value) {
            field = value
            onParamsChanged()
        }

    private val waves = mutableListOf<WaveParams>()
    private val color = ContextCompat.getColor(context, R.color.car_icon_bounds_color)

    private val enabled = AtomicBoolean(false)
    private val anims = mutableListOf<AnimatorSet>()
    private val handler = Handler(Looper.getMainLooper())
    override fun draw(canvas: Canvas) {
        val cartesianPosition = getCartesianPosition()
        waves.toList().forEach {
            canvas.drawCircle(cartesianPosition.x, cartesianPosition.y, it.radius, it.paint)
        }
    }

    data class WaveParams(
        val paint: Paint = Paint(),
        var radius: Float = 0f,
        // 模糊
    )

    override fun start(vararg params: Any) {
        if (anims.isNotEmpty()) return
        enabled.set(true)
        require(params.size == 2)
        startInternal(params[0] as Float, params[1] as Float)
    }

    private fun startInternal(scaleStart: Float, scaleEnd: Float) {
        if (enabled.get().not()) return
        val wave = WaveParams(
            Paint().apply {
                color = this@CarWaterWave.color
                style = Paint.Style.FILL
            },
            radius = dimens.carIconBgRadius.toFloat()
        )
        val enlargeAnim = createEnlargeAnim(scaleStart, scaleEnd)
        enlargeAnim.addUpdateListener {
            val scale = it.animatedValue as Float
            wave.radius = dimens.carIconBgRadius * params.scale * scale
            naviView.invalidate()
        }
        val fadeInAnim = createFadeInAnim()
        fadeInAnim.addUpdateListener {
            val alpha = it.animatedValue as Float
            wave.paint.alpha = (alpha * 255).toInt()
            naviView.invalidate()
        }
        val fadeOutAnim = createFadeOutAnim()
        fadeOutAnim.addUpdateListener {
            val alpha = it.animatedValue as Float
            wave.paint.alpha = (alpha * 255).toInt()
            naviView.invalidate()
        }
        val blurAnim = createBlurAnim()
        blurAnim.addUpdateListener {
            val alpha = it.animatedValue as Float
            wave.paint.maskFilter =
                BlurMaskFilter(dimens.carIconBgRadius * alpha, BlurMaskFilter.Blur.NORMAL)
        }
        val animSet = AnimatorSet()
        animSet.doOnEnd {
            waves.remove(wave)
            anims.remove(animSet)
        }
        animSet.doOnStart {
            waves.add(wave)
            anims.add(animSet)
        }
        animSet.playTogether(enlargeAnim, fadeInAnim, fadeOutAnim)
        animSet.start()

        handler.postDelayed({ startInternal(scaleStart, scaleEnd) }, 1000)
    }

    private fun createEnlargeAnim(scaleStart: Float, scaleEnd: Float): ValueAnimator {
        return ValueAnimator.ofFloat(scaleStart, scaleEnd).apply {
            duration = 4500
            interpolator = scaleInterpolator
        }
    }

    private fun createFadeInAnim(): ValueAnimator {
        return ValueAnimator.ofFloat(0f, 0.2f).apply {
            duration = 500
            interpolator = alphaInterpolator
        }
    }

    private fun createFadeOutAnim(): ValueAnimator {
        return ValueAnimator.ofFloat(0.2f, 0f).apply {
            duration = 2500
            interpolator = alphaInterpolator
            startDelay = 2000
        }
    }

    private fun createBlurAnim(): ValueAnimator {
        return ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2500
            interpolator = alphaInterpolator
            startDelay = 2000
        }
    }

    override fun stop() {
        enabled.set(false)
        waves.clear()
        anims.toList().forEach(AnimatorSet::cancel)
        handler.removeCallbacksAndMessages(null)
    }

}