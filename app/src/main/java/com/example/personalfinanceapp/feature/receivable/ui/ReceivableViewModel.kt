package com.example.personalfinanceapp.feature.receivable.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.personalfinanceapp.core.model.CurrencyCode
import com.example.personalfinanceapp.core.model.Receivable
import com.example.personalfinanceapp.core.model.UserId
import com.example.personalfinanceapp.feature.auth.AuthRepository
import com.example.personalfinanceapp.feature.income.IncomeRepository
import com.example.personalfinanceapp.feature.receivable.ReceivableRepository
import com.example.personalfinanceapp.feature.receivable.ReceivableSettlement
import com.example.personalfinanceapp.feature.receivable.ReceivableSettlementEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class ReceivableUiState(
    val isLoading: Boolean = true,
    val ownerId: UserId? = null,
    val settlements: List<ReceivableSettlement> = emptyList(),
    val errorMessage: String? = null,
    val operationMessage: String? = null
)

data class ReceivableFormState(
    val id: String,
    val ownerId: UserId,
    val isEditing: Boolean,
    val title: String = "",
    val projectReference: String = "",
    val amount: String = "",
    val currency: CurrencyCode = CurrencyCode.LKR,
    val expectedDate: String,
    val archived: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ReceivableViewModel @Inject constructor(
    private val receivableRepository: ReceivableRepository,
    private val incomeRepository: IncomeRepository,
    private val authRepository: AuthRepository,
    private val clock: Clock
) : ViewModel() {
    private val retry = MutableStateFlow(0L)
    private val _formState = MutableStateFlow<ReceivableFormState?>(null)
    val formState: StateFlow<ReceivableFormState?> = _formState
    private val authenticatedUserId = authRepository.observeAuthState()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            var previousOwner: UserId? = null
            authenticatedUserId.collectLatest { owner ->
                if (previousOwner != owner) {
                    _formState.value = null
                    previousOwner = owner
                }
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<ReceivableUiState> = combine(
        authenticatedUserId, retry
    ) { owner, version -> owner to version }
        .flatMapLatest { (owner, _) ->
            if (owner.isNullOrBlank()) flowOf(ReceivableUiState(isLoading = false))
            else observeOwner(owner)
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReceivableUiState())

    private fun observeOwner(owner: UserId): Flow<ReceivableUiState> = combine(
        receivableRepository.observeReceivableSnapshots(),
        incomeRepository.observeIncomeSnapshots(
            LocalDate.of(1900, 1, 1), LocalDate.of(3000, 1, 1)
        )
    ) { receivableSnapshot, incomeSnapshot ->
        if (receivableSnapshot.ownerId != owner || incomeSnapshot.ownerId != owner) {
            ReceivableUiState(isLoading = true)
        } else {
            val settlements = receivableSnapshot.receivables.map { receivable ->
                ReceivableSettlementEngine.calculate(receivable, incomeSnapshot.incomes).getOrThrow()
            }
            ReceivableUiState(
                isLoading = false,
                ownerId = owner,
                settlements = settlements.sortedWith(
                    compareBy<ReceivableSettlement> { it.receivable.archived }
                        .thenBy { it.receivable.expectedDate }
                        .thenBy { it.receivable.id }
                )
            )
        }
    }.onStart { emit(ReceivableUiState(isLoading = true)) }
        .catch { failure ->
            if (failure is CancellationException) throw failure
            emit(ReceivableUiState(isLoading = false, ownerId = owner, errorMessage = "Unable to load receivables. Please retry."))
        }

    fun retry() = retry.update { if (it == Long.MAX_VALUE) 0L else it + 1L }

    fun openCreate() {
        val owner = uiState.value.ownerId ?: return
        _formState.value = ReceivableFormState(
            id = UUID.randomUUID().toString(),
            ownerId = owner,
            isEditing = false,
            expectedDate = LocalDate.now(clock).toString()
        )
    }

    fun openEdit(receivable: Receivable) {
        val owner = uiState.value.ownerId ?: return
        if (uiState.value.settlements.none { it.receivable == receivable }) return
        _formState.value = ReceivableFormState(
            id = receivable.id,
            ownerId = owner,
            isEditing = true,
            title = receivable.title,
            projectReference = receivable.projectReference,
            amount = receivable.expectedOriginalDecimal,
            currency = receivable.currency,
            expectedDate = receivable.expectedDate.toString(),
            archived = receivable.archived
        )
    }

    fun closeForm() { if (_formState.value?.isSaving != true) _formState.value = null }
    fun updateTitle(value: String) = updateForm { it.copy(title = value, errorMessage = null) }
    fun updateProject(value: String) = updateForm { it.copy(projectReference = value, errorMessage = null) }
    fun updateAmount(value: String) = updateForm { it.copy(amount = value, errorMessage = null) }
    fun updateCurrency(value: CurrencyCode) = updateForm { it.copy(currency = value, errorMessage = null) }
    fun updateDate(value: String) = updateForm { it.copy(expectedDate = value, errorMessage = null) }

    fun save() {
        val form = _formState.value ?: return
        val owner = form.ownerId
        if (authenticatedUserId.value != owner || uiState.value.ownerId != owner) return
        val date = runCatching { LocalDate.parse(form.expectedDate.trim()) }.getOrNull()
        if (date == null) {
            _formState.value = form.copy(errorMessage = "Enter a valid date as YYYY-MM-DD")
            return
        }
        val receivable = Receivable(
            form.id, form.title, form.projectReference, form.amount,
            form.currency, date, form.archived
        )
        _formState.value = form.copy(isSaving = true, errorMessage = null)
        viewModelScope.launch {
            val result = receivableRepository.saveReceivableForOwner(owner, receivable)
            val current = _formState.value
            if (
                current?.id != form.id || current.ownerId != owner ||
                authenticatedUserId.value != owner || uiState.value.ownerId != owner
            ) return@launch
            result.fold(
                onSuccess = { _formState.value = null },
                onFailure = { _formState.value = current.copy(isSaving = false, errorMessage = it.message ?: "Unable to save receivable") }
            )
        }
    }

    fun setArchived(receivable: Receivable, archived: Boolean) {
        val owner = uiState.value.ownerId ?: return
        if (authenticatedUserId.value != owner) return
        if (uiState.value.settlements.none { it.receivable == receivable }) return
        viewModelScope.launch {
            receivableRepository.setReceivableArchivedForOwner(owner, receivable.id, archived)
        }
    }

    private fun updateForm(block: (ReceivableFormState) -> ReceivableFormState) {
        _formState.update { if (it == null || it.isSaving) it else block(it) }
    }
}
