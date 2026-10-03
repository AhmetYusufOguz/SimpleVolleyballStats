package org.ahmetyusufoguz.simplevolleyballstats.presentation.screens.team

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.Player
import org.ahmetyusufoguz.simplevolleyballstats.domain.model.PlayerPosition
import org.ahmetyusufoguz.simplevolleyballstats.domain.repository.PlayerRepository

data class AddPlayerState(
    val number: String = "",
    val name: String = "",
    val position: PlayerPosition? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val isValid: Boolean = false
)

class AddPlayerViewModel(
    private val teamId: String,
    private val playerRepository: PlayerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddPlayerState())
    val state: StateFlow<AddPlayerState> = _state.asStateFlow()

    fun updateNumber(value: String) {
        _state.update { it.copy(number = value) }
        validateForm()
    }

    fun updateName(value: String) {
        _state.update { it.copy(name = value) }
        validateForm()
    }

    fun updatePosition(value: PlayerPosition?) {
        _state.update { it.copy(position = value) }
    }

    private fun validateForm() {
        val currentState = _state.value
        val isValid = currentState.name.isNotBlank() &&
                currentState.number.isNotBlank() &&
                currentState.number.toIntOrNull() != null &&
                currentState.number.toInt() in 1..99

        _state.update { it.copy(isValid = isValid) }
    }

    fun savePlayer(onSuccess: () -> Unit) {
        val currentState = _state.value
        if (!currentState.isValid) return

        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }

            try {
                val player = Player.create(
                    teamId = teamId,
                    number = currentState.number.toInt(),
                    name = currentState.name.trim(),
                    position = currentState.position
                )
                playerRepository.addPlayer(player)
                onSuccess()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message, isSaving = false) }
            }
        }
    }
}