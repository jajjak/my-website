package com.linnan.hayaophoto.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.linnan.hayaophoto.R
import com.linnan.hayaophoto.data.PersonEntity
import com.linnan.hayaophoto.ui.Routes
import com.linnan.hayaophoto.ui.components.PersonCard
import com.linnan.hayaophoto.viewmodel.PersonViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    var selectedTab by remember { mutableStateOf(0) }
    val titles = listOf(stringResource(R.string.app_name), "検索", "設定")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titles[selectedTab]) },
                actions = {
                    if (selectedTab == 0) {
                        androidx.compose.material3.IconButton(onClick = { navController.navigate(Routes.camera()) }) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = "撮影")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { navController.navigate(Routes.addEditPerson()) }) {
                    Icon(Icons.Filled.Add, contentDescription = "人物を追加")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = null) },
                    label = { Text("人物") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("検索") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    label = { Text("設定") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> PersonListContent(navController)
                1 -> SearchTabContent(navController)
                else -> SettingsTabContent(navController)
            }
        }
    }
}

@Composable
private fun PersonListContent(navController: NavHostController) {
    val viewModel: PersonViewModel = viewModel()
    val persons by viewModel.persons.collectAsState()
    var pendingDelete by remember { mutableStateOf<PersonEntity?>(null) }

    if (persons.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Text(
                "右下の + から、写真を残したい人を登録しましょう",
                modifier = Modifier
                    .padding(32.dp)
                    .align(androidx.compose.ui.Alignment.Center),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(persons, key = { it.id }) { person ->
                PersonCard(
                    person = person,
                    onClick = { navController.navigate(Routes.personDetail(person.id)) },
                    onLongPress = { pendingDelete = person }
                )
            }
        }
    }

    pendingDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("${person.name} を削除しますか？") },
            text = { Text("登録されている写真・動画もすべて削除されます。この操作は取り消せません。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePerson(person)
                    pendingDelete = null
                }) { Text("削除する", color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("キャンセル") }
            }
        )
    }
}
