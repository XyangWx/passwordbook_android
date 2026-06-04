package com.mksword.passwordbook

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mksword.passwordbook.entities.NewPasswordBookRequest
import com.mksword.passwordbook.entities.PasswordBook
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    userName: String,
    passwordBooks: List<PasswordBook>,
    isListLoading: Boolean,
    isLoggingOut: Boolean = false,
    onLogoutClick: () -> Unit,
    onRefreshList: () -> Unit
) {
    // 页面与表单控制状态
    var isCreatingNewBook by remember { mutableStateOf(false) }
    var currentNewBookRequest by remember { mutableStateOf<NewPasswordBookRequest?>(null) }
    var isFormValid by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    // 弹窗与二级页面切换状态
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedPasswordBook by remember { mutableStateOf<PasswordBook?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var currentViewBookId by remember { mutableStateOf<String?>(null) }
    var detailRefreshKey by remember { mutableStateOf(0) }

    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            PasswordBookHeader(userName = userName, isLoggingOut = isLoggingOut, onLogoutClick = onLogoutClick)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // 调度并渲染底栏
            MainAppFooterDispatcher(
                isCreatingNewBook = isCreatingNewBook,
                isFormValid = isFormValid,
                isSubmitting = isSubmitting,
                currentNewBookRequest = currentNewBookRequest,
                currentViewBookId = currentViewBookId,
                snackbarHostState = snackbarHostState,
                onRefreshList = onRefreshList,
                onRefreshDetail = { detailRefreshKey++ },
                onCloseCreateMode = { isCreatingNewBook = false; currentNewBookRequest = null },
                onOpenCreateMode = { isCreatingNewBook = true },
                onCloseDetailMode = { currentViewBookId = null }
            )
        }
    ) { innerPadding ->
        // 调度并渲染主体 Body
        when {
            isCreatingNewBook -> {
                CreatePasswordBookBody(
                    modifier = Modifier.padding(innerPadding),
                    onFormChange = { request, isValid ->
                        if (!isSubmitting) { currentNewBookRequest = request; isFormValid = isValid }
                    }
                )
            }
            currentViewBookId != null -> {
                ViewPasswordBookDetailBody(
                    modifier = Modifier.padding(innerPadding),
                    passwordBookId = currentViewBookId!!,
                    refreshKey = detailRefreshKey
                )
            }
            else -> {
                PasswordBookBody(
                    modifier = Modifier.padding(innerPadding),
                    passwordBooks = passwordBooks,
                    isListLoading = isListLoading,
                    onBookClick = { bookId ->
                        val targetBook = passwordBooks.find { it.id == bookId }
                        if (targetBook != null) {
                            selectedPasswordBook = targetBook
                            showBottomSheet = true
                        }
                    }
                )
            }
        }

        // 引入抽离出来的底部操作弹窗
        PasswordBookActionBottomSheet(
            showBottomSheet = showBottomSheet,
            sheetState = sheetState,
            selectedBookName = selectedPasswordBook?.name ?: "",
            onDismiss = { showBottomSheet = false },
            onViewClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showBottomSheet = false
                        currentViewBookId = selectedPasswordBook?.id
                    }
                }
            },
            onDeleteClick = {
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        showBottomSheet = false
                        showDeleteDialog = true
                    }
                }
            }
        )

        // 引入抽离出来的删除确认框
        PasswordBookDeleteDialog(
            showDialog = showDeleteDialog,
            selectedBook = selectedPasswordBook,
            snackbarHostState = snackbarHostState,
            onDismiss = { showDeleteDialog = false },
            onDeleteSuccess = {
                showDeleteDialog = false
                selectedPasswordBook = null
                onRefreshList()
            }
        )
    }
}