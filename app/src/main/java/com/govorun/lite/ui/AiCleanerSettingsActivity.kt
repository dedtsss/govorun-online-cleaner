package com.govorun.lite.ui

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.color.DynamicColors
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.govorun.lite.util.AiCleanerPrefs
import com.govorun.lite.util.GigaChatClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Settings screen for optional online AI cleanup of already-recognized text. */
class AiCleanerSettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyEdit: TextInputEditText
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val toolbar = MaterialToolbar(this).apply {
            title = "AI-очистка"
            setNavigationIcon(com.govorun.lite.R.drawable.ic_arrow_back_24)
            setNavigationOnClickListener { finish() }
        }
        root.addView(toolbar, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            resources.getDimensionPixelSize(com.google.android.material.R.dimen.abc_action_bar_default_height_material)
        ))

        val scroll = ScrollView(this).apply { isFillViewport = false }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(24))
        }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        content.addView(sectionTitle("Онлайн-очистка текста"))
        content.addView(bodyText(
            "Распознавание речи остаётся офлайн через GigaAM. " +
                "В сеть отправляется только уже распознанный текст и только если эта функция включена."
        ))

        val enabledSwitch = MaterialSwitch(this).apply {
            isChecked = AiCleanerPrefs.isEnabled(this@AiCleanerSettingsActivity)
        }
        val enabledRow = horizontalRow(
            title = "Включить AI-очистку",
            body = "После распознавания можно будет очистить текст через GigaChat.",
            trailing = enabledSwitch
        )
        enabledRow.setOnClickListener { enabledSwitch.toggle() }
        enabledSwitch.setOnCheckedChangeListener { _, checked ->
            AiCleanerPrefs.setEnabled(this, checked)
        }
        content.addView(enabledRow)

        content.addView(spacer(16))
        content.addView(sectionTitle("GigaChat API"))
        content.addView(bodyText(
            "Вставьте Authorization Key из личного кабинета Sber Developers. " +
                "Не вставляйте сюда временный access token."
        ))

        apiKeyEdit = TextInputEditText(this).apply {
            setText(AiCleanerPrefs.getAuthorizationKey(this@AiCleanerSettingsActivity))
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setSingleLine(true)
        }
        val apiKeyLayout = TextInputLayout(this).apply {
            hint = "GigaChat Authorization Key"
            endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            addView(apiKeyEdit)
        }
        content.addView(apiKeyLayout)

        val saveKeyButton = MaterialButton(this).apply {
            text = "Сохранить ключ"
            setOnClickListener {
                AiCleanerPrefs.setAuthorizationKey(
                    this@AiCleanerSettingsActivity,
                    apiKeyEdit.text?.toString().orEmpty()
                )
                Toast.makeText(this@AiCleanerSettingsActivity, "Ключ сохранён", Toast.LENGTH_SHORT).show()
            }
        }
        content.addView(saveKeyButton)
        val checkButton = MaterialButton(this).apply {
            text = "Проверить GigaChat"
            setOnClickListener {
                AiCleanerPrefs.setAuthorizationKey(
                    this@AiCleanerSettingsActivity,
                    apiKeyEdit.text?.toString().orEmpty()
                )
                isEnabled = false
                scope.launch(Dispatchers.IO) {
                    val result = try {
                        GigaChatClient(this@AiCleanerSettingsActivity).checkConnection()
                        "Проверка успешна: GigaChat доступен."
                    } catch (e: GigaChatClient.Error.MissingAuthorizationKey) {
                        "Ошибка: не указан Authorization Key."
                    } catch (e: GigaChatClient.Error.Unauthorized) {
                        "Ошибка ключа: Authorization Key недействителен или отозван."
                    } catch (e: GigaChatClient.Error.Tls) {
                        "TLS/SSL ошибка подключения. Проверьте сертификаты, VPN и дату/время."
                    } catch (e: GigaChatClient.Error.Timeout) {
                        "Ошибка сети: таймаут подключения."
                    } catch (e: GigaChatClient.Error.Network) {
                        "Ошибка сети: не удалось подключиться к GigaChat."
                    } catch (e: GigaChatClient.Error.RateLimited) {
                        "Ошибка API: слишком много запросов (HTTP 429)."
                    } catch (e: GigaChatClient.Error.ServerUnavailable) {
                        "Ошибка API: сервер временно недоступен (HTTP 5xx)."
                    } catch (e: GigaChatClient.Error.EmptyResponse) {
                        "Ошибка API: получен пустой ответ."
                    } catch (e: GigaChatClient.Error.Api) {
                        "Ошибка API: ${e.message}"
                    } catch (e: Exception) {
                        "Ошибка: ${e.message ?: "неизвестная ошибка"}"
                    }
                    scope.launch(Dispatchers.Main) {
                        isEnabled = true
                        Toast.makeText(this@AiCleanerSettingsActivity, result, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        content.addView(checkButton)

        content.addView(spacer(16))
        content.addView(sectionTitle("Модель"))
        val modelGroup = MaterialButtonToggleGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        val liteId = View.generateViewId()
        val proId = View.generateViewId()
        modelGroup.addView(toggleButton(liteId, "2 Lite"))
        modelGroup.addView(toggleButton(proId, "2 Pro"))
        modelGroup.check(
            if (AiCleanerPrefs.getModel(this) == AiCleanerPrefs.MODEL_GIGACHAT_2_PRO) proId else liteId
        )
        modelGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            AiCleanerPrefs.setModel(
                this,
                if (checkedId == proId) AiCleanerPrefs.MODEL_GIGACHAT_2_PRO else AiCleanerPrefs.MODEL_GIGACHAT_2_LITE
            )
        }
        content.addView(modelGroup)
        content.addView(bodyText("Lite — дешевле и быстрее. Pro — качественнее для аккуратной правки."))

        content.addView(spacer(16))
        content.addView(sectionTitle("Режим правки"))
        val modeGroup = MaterialButtonToggleGroup(this).apply {
            isSingleSelection = true
            isSelectionRequired = true
        }
        val lightId = View.generateViewId()
        val normalId = View.generateViewId()
        val cleanId = View.generateViewId()
        modeGroup.addView(toggleButton(lightId, "Лёгкая"))
        modeGroup.addView(toggleButton(normalId, "Нормальная"))
        modeGroup.addView(toggleButton(cleanId, "Аккуратная"))
        modeGroup.check(when (AiCleanerPrefs.getMode(this)) {
            AiCleanerPrefs.MODE_LIGHT -> lightId
            AiCleanerPrefs.MODE_CLEAN -> cleanId
            else -> normalId
        })
        modeGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = when (checkedId) {
                lightId -> AiCleanerPrefs.MODE_LIGHT
                cleanId -> AiCleanerPrefs.MODE_CLEAN
                else -> AiCleanerPrefs.MODE_NORMAL
            }
            AiCleanerPrefs.setMode(this, mode)
        }
        content.addView(modeGroup)
        content.addView(bodyText(
            "Лёгкая — пунктуация и явные ошибки. Нормальная — ещё повторы и слова-паразиты. " +
                "Аккуратная — сделать текст более читаемым без изменения смысла."
        ))

        setContentView(root)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun sectionTitle(text: String): MaterialTextView =
        MaterialTextView(this).apply {
            this.text = text
            textSize = 18f
            setPadding(0, dp(8), 0, dp(4))
        }

    private fun bodyText(text: String): MaterialTextView =
        MaterialTextView(this).apply {
            this.text = text
            textSize = 14f
            setPadding(0, dp(2), 0, dp(10))
        }

    private fun horizontalRow(title: String, body: String, trailing: View): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            background = obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground)).use { attrs ->
                attrs.getDrawable(0)
            }
            isClickable = true
            isFocusable = true

            val texts = LinearLayout(this@AiCleanerSettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            texts.addView(MaterialTextView(this@AiCleanerSettingsActivity).apply {
                text = title
                textSize = 16f
            })
            texts.addView(MaterialTextView(this@AiCleanerSettingsActivity).apply {
                text = body
                textSize = 14f
            })
            addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            addView(trailing)
        }

    private fun toggleButton(id: Int, text: String): MaterialButton =
        MaterialButton(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            this.id = id
            this.text = text
        }

    private fun spacer(heightDp: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(heightDp)
        )
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private inline fun <T> android.content.res.TypedArray.use(block: (android.content.res.TypedArray) -> T): T {
        return try { block(this) } finally { recycle() }
    }
}
