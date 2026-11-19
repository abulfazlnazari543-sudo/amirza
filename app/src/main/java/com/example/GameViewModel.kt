package com.example

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class GameScreen {
    MAIN_MENU,
    PLAYING,
    LEVEL_SELECT,
    VICTORY
}

data class TargetWordState(
    val word: String,
    val isSolved: Boolean,
    val revealedIndices: Set<Int> // positions of characters revealed (via hints or fully solved)
)

data class GameState(
    val currentScreen: GameScreen = GameScreen.MAIN_MENU,
    val coins: Int = 500,
    val currentLevelIndex: Int = 0,
    val highestUnlockedLevel: Int = 0, // index of highest unlocked level
    val targetWords: List<TargetWordState> = emptyList(),
    val bonusWordsFound: List<String> = emptyList(),
    val currentLetters: List<Char> = emptyList(), // current arrangement (supports shuffling)
    val candidateWord: String = "",
    val feedbackMessage: String? = null,
    val feedbackSuccess: Boolean = false,
    val showLevelCompleteDialog: Boolean = false,
    val particlesTrigger: Int = 0 // increments to animate victory sparkles
)

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("amirza_game_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(GameState())
    val uiState: StateFlow<GameState> = _uiState.asStateFlow()

    init {
        loadProgress()
    }

    private fun loadProgress() {
        val coins = prefs.getInt("coins", 500)
        val levelIndex = prefs.getInt("current_level_index", 0)
        val highest = prefs.getInt("highest_unlocked_level", 0)

        _uiState.update {
            it.copy(
                coins = coins,
                currentLevelIndex = levelIndex.coerceIn(0, GameData.levels.size - 1),
                highestUnlockedLevel = highest.coerceIn(0, GameData.levels.size - 1)
            )
        }
        loadLevel(levelIndex)
    }

    private fun saveProgress() {
        val state = _uiState.value
        prefs.edit().apply {
            putInt("coins", state.coins)
            putInt("current_level_index", state.currentLevelIndex)
            putInt("highest_unlocked_level", state.highestUnlockedLevel)
            
            // Save solved words for current level
            val solvedWords = state.targetWords.filter { it.isSolved }.map { it.word }
            putStringSet("solved_words_${state.currentLevelIndex}", solvedWords.toSet())
            
            // Save revealed indices for unsolved target words
            state.targetWords.forEachIndexed { wordIndex, target ->
                if (!target.isSolved && target.revealedIndices.isNotEmpty()) {
                    putStringSet(
                        "revealed_indices_${state.currentLevelIndex}_$wordIndex",
                        target.revealedIndices.map { it.toString() }.toSet()
                    )
                } else {
                    remove("revealed_indices_${state.currentLevelIndex}_$wordIndex")
                }
            }

            // Save bonus words
            putStringSet("bonus_words_${state.currentLevelIndex}", state.bonusWordsFound.toSet())
            apply()
        }
    }

    fun loadLevel(levelIndex: Int) {
        val level = GameData.levels.getOrNull(levelIndex) ?: return
        
        // Load solved target words
        val savedSolved = prefs.getStringSet("solved_words_$levelIndex", emptySet()) ?: emptySet()
        val savedBonus = prefs.getStringSet("bonus_words_$levelIndex", emptySet()) ?: emptySet()

        val targetStates = level.targetWords.mapIndexed { wordIndex, word ->
            val isSolved = savedSolved.contains(word)
            val revealedIndices = if (isSolved) {
                word.indices.toSet()
            } else {
                // Read saved hints
                val hints = prefs.getStringSet("revealed_indices_${levelIndex}_$wordIndex", emptySet()) ?: emptySet()
                hints.mapNotNull { it.toIntOrNull() }.toSet()
            }
            TargetWordState(
                word = word,
                isSolved = isSolved,
                revealedIndices = revealedIndices
            )
        }

        _uiState.update {
            it.copy(
                currentLevelIndex = levelIndex,
                targetWords = targetStates,
                bonusWordsFound = savedBonus.toList(),
                currentLetters = level.letters, // Starts unshuffled
                candidateWord = "",
                feedbackMessage = null,
                showLevelCompleteDialog = false
            )
        }
    }

    fun setScreen(screen: GameScreen) {
        _uiState.update { it.copy(currentScreen = screen, feedbackMessage = null) }
    }

    fun selectLevel(levelIndex: Int) {
        loadLevel(levelIndex)
        _uiState.update { it.copy(currentScreen = GameScreen.PLAYING) }
    }

    fun shuffleLetters() {
        _uiState.update { state ->
            state.copy(currentLetters = state.currentLetters.shuffled())
        }
    }

    fun buildCandidateFromIndices(indices: List<Int>) {
        val letters = _uiState.value.currentLetters
        val word = indices.mapNotNull { letters.getOrNull(it) }.joinToString("")
        _uiState.update { it.copy(candidateWord = word) }
    }

    fun addLetterToCandidate(char: Char) {
        _uiState.update { it.copy(candidateWord = it.candidateWord + char) }
    }

    fun removeLastLetter() {
        _uiState.update {
            if (it.candidateWord.isNotEmpty()) {
                it.copy(candidateWord = it.candidateWord.dropLast(1))
            } else it
        }
    }

    fun clearCandidate() {
        _uiState.update { it.copy(candidateWord = "") }
    }

    fun submitCandidate() {
        val word = _uiState.value.candidateWord.trim()
        if (word.isEmpty()) return

        val level = GameData.levels[_uiState.value.currentLevelIndex]

        // 1. Matches Target Words?
        val targetIndex = level.targetWords.indexOf(word)
        if (targetIndex != -1) {
            val targetState = _uiState.value.targetWords[targetIndex]
            if (targetState.isSolved) {
                // Already solved
                showFeedback("این کلمه را قبلاً حدس زده‌اید!", false)
                _uiState.update { it.copy(candidateWord = "") }
                return
            } else {
                // Correct target word found!
                val updatedTargetWords = _uiState.value.targetWords.toMutableList()
                updatedTargetWords[targetIndex] = targetState.copy(
                    isSolved = true,
                    revealedIndices = word.indices.toSet()
                )

                _uiState.update {
                    it.copy(
                        targetWords = updatedTargetWords,
                        candidateWord = "",
                        feedbackMessage = "آفرین! درست بود ✨",
                        feedbackSuccess = true,
                        particlesTrigger = it.particlesTrigger + 1
                    )
                }

                checkLevelCompletion()
                saveProgress()
                return
            }
        }

        // 2. Matches Bonus Words?
        if (level.bonusWords.contains(word)) {
            if (_uiState.value.bonusWordsFound.contains(word)) {
                showFeedback("این کلمه اضافی را قبلاً پیدا کرده‌اید!", false)
            } else {
                val updatedBonus = _uiState.value.bonusWordsFound + word
                _uiState.update {
                    it.copy(
                        bonusWordsFound = updatedBonus,
                        coins = it.coins + 15,
                        feedbackMessage = "کلمه اضافی پیدا شد! ۱۵+ سکه 💰",
                        feedbackSuccess = true,
                        particlesTrigger = it.particlesTrigger + 1
                    )
                }
                saveProgress()
            }
            _uiState.update { it.copy(candidateWord = "") }
            return
        }

        // 3. Incorrect word
        showFeedback("کلمه درستی نیست ❌", false)
        _uiState.update { it.copy(candidateWord = "") }
    }

    private fun checkLevelCompletion() {
        val allSolved = _uiState.value.targetWords.all { it.isSolved }
        if (allSolved) {
            // Reward level completion
            val newCoins = _uiState.value.coins + 50
            val nextLevel = _uiState.value.currentLevelIndex + 1
            val maxLevels = GameData.levels.size
            
            val updatedHighest = if (nextLevel < maxLevels && nextLevel > _uiState.value.highestUnlockedLevel) {
                nextLevel
            } else {
                _uiState.value.highestUnlockedLevel
            }

            _uiState.update {
                it.copy(
                    coins = newCoins,
                    highestUnlockedLevel = updatedHighest,
                    showLevelCompleteDialog = true,
                    feedbackMessage = "تبریک! تمام کلمات پیدا شدند 🎖️",
                    feedbackSuccess = true
                )
            }
            saveProgress()
        }
    }

    fun nextLevel() {
        val nextLevelIndex = _uiState.value.currentLevelIndex + 1
        if (nextLevelIndex < GameData.levels.size) {
            _uiState.update {
                it.copy(
                    currentLevelIndex = nextLevelIndex,
                    showLevelCompleteDialog = false,
                    candidateWord = ""
                )
            }
            loadLevel(nextLevelIndex)
            saveProgress()
        } else {
            // Completed all levels! Show Victory Screen!
            _uiState.update {
                it.copy(
                    currentScreen = GameScreen.VICTORY,
                    showLevelCompleteDialog = false
                )
            }
        }
    }

    fun useHint() {
        val cost = 100
        if (_uiState.value.coins < cost) {
            showFeedback("سکه‌های شما کافی نیست! (هزینه راهنما: ۱۰۰ سکه)", false)
            return
        }

        // Search for all eligible unrevealed indices in unsolved target words
        val candidates = mutableListOf<Pair<Int, Int>>() // list of Pair(wordIndex, charIndex)
        _uiState.value.targetWords.forEachIndexed { wordIndex, target ->
            if (!target.isSolved) {
                target.word.forEachIndexed { charIndex, _ ->
                    if (!target.revealedIndices.contains(charIndex)) {
                        candidates.add(wordIndex to charIndex)
                    }
                }
            }
        }

        if (candidates.isEmpty()) {
            // All revealed, but perhaps not fully submitted (edge case)
            return
        }

        // Select a random candidate position
        val selected = candidates.random()
        val wordIndex = selected.first
        val charIndex = selected.second

        val updatedTargetWords = _uiState.value.targetWords.toMutableList()
        val currentTarget = updatedTargetWords[wordIndex]
        val newRevealed = currentTarget.revealedIndices + charIndex
        
        // If this action completes the word, set solved to true
        val isWordNowSolved = newRevealed.size == currentTarget.word.length

        updatedTargetWords[wordIndex] = currentTarget.copy(
            revealedIndices = newRevealed,
            isSolved = isWordNowSolved
        )

        _uiState.update {
            it.copy(
                coins = it.coins - cost,
                targetWords = updatedTargetWords,
                feedbackMessage = "یک حرف فاش شد! ✨" + if (isWordNowSolved) " (کلمه کامل شد!)" else "",
                feedbackSuccess = true
            )
        }

        if (isWordNowSolved) {
            checkLevelCompletion()
        }
        saveProgress()
    }

    private fun showFeedback(message: String, success: Boolean) {
        _uiState.update {
            it.copy(
                feedbackMessage = message,
                feedbackSuccess = success
            )
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
    
    fun resetGame() {
        prefs.edit().clear().apply()
        _uiState.update {
            GameState(
                currentScreen = GameScreen.MAIN_MENU,
                coins = 500,
                currentLevelIndex = 0,
                highestUnlockedLevel = 0
            )
        }
        loadLevel(0)
    }
}
