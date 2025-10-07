# Tempo - Jetpack Compose Migration

## 📱 Sobre esta Migração

Este diretório contém a nova implementação do Tempo usando **Kotlin** e **Jetpack Compose**, inspirada no design e arquitetura do **Metrolist**.

## 🏗️ Estrutura do Projeto

```
app/src/main/kotlin/com/ShiromiyaG/tempo/
│
├── ui/
│   ├── component/              # Componentes reutilizáveis
│   │   ├── shimmer/           # Loading placeholders
│   │   ├── EmptyPlaceholder.kt
│   │   ├── Items.kt           # SongListItem, GridItem
│   │   ├── NavigationTitle.kt
│   │   ├── PlayerSlider.kt
│   │   └── SearchBar.kt
│   │
│   ├── screens/               # Telas principais
│   │   ├── library/          # Biblioteca (songs, albums, artists, playlists)
│   │   ├── settings/         # Configurações
│   │   └── HomeScreen.kt
│   │
│   ├── player/               # Player de música
│   │   └── MiniPlayer.kt
│   │
│   ├── menu/                 # Menus e dialogs
│   │
│   ├── theme/                # Tema e estilos
│   │   ├── Theme.kt
│   │   └── Type.kt
│   │
│   └── utils/                # Utilitários
│
├── viewmodel/                # ViewModels (Kotlin)
├── repository/               # Repositórios
├── database/                 # Room database
└── service/                  # MediaService (Media3)
```

## 🎨 Design System

### Material 3
- ✅ Design system moderno do Android
- ✅ Dynamic color support (Android 12+)
- ✅ Temas dark e light
- ✅ Componentes consistentes

### Tipografia
- Fonte: **Circular Std** (mesma do Tempo original)
- Escalas de texto do Material3
- Hierarquia visual clara

### Cores
- Paleta inspirada no Metrolist
- Suporte a cores dinâmicas do sistema
- Alto contraste para acessibilidade

## 🧩 Componentes Principais

### EmptyPlaceholder
Estado vazio com ícone e mensagem
```kotlin
EmptyPlaceholder(
    icon = Icons.Rounded.MusicNote,
    text = "No music found"
)
```

### SongListItem
Item de lista para músicas
```kotlin
SongListItem(
    title = "Song Name",
    artist = "Artist Name",
    thumbnailUrl = "...",
    onClick = { }
)
```

### GridItem
Item de grade para álbuns/artistas
```kotlin
GridItem(
    title = "Album Name",
    subtitle = "Artist Name",
    thumbnailUrl = "...",
    isCircular = false
)
```

### SearchBar
Barra de pesquisa Material3
```kotlin
SearchBar(
    query = searchQuery,
    onQueryChange = { },
    onSearch = { }
)
```

### MiniPlayer
Player compacto na parte inferior
```kotlin
MiniPlayer(
    title = "Song",
    artist = "Artist",
    isPlaying = true,
    progress = 0.5f,
    onPlayPauseClick = { }
)
```

## 🚀 Como Começar

### 1. Sync do Projeto
```bash
./gradlew clean build
```

### 2. Executar o App
- Abra no Android Studio
- Selecione a configuração `tempo`
- Run (ou Shift+F10)

### 3. Testar a Nova UI

Para testar a nova UI Compose:

1. Edite `AndroidManifest.xml`
2. Altere a Activity principal para `ComposeMainActivity`
3. Ou crie uma nova Activity launcher

Exemplo:
```xml
<activity
    android:name=".ui.ComposeMainActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

## 📚 Documentação

- **[MIGRATION_GUIDE.md](../MIGRATION_GUIDE.md)**: Guia completo de migração
- **[COMPOSE_EXAMPLES.md](../COMPOSE_EXAMPLES.md)**: Exemplos de uso dos componentes
- **[MIGRATION_CHECKLIST.md](../MIGRATION_CHECKLIST.md)**: Checklist de progresso

## 🔧 Tecnologias Utilizadas

### Core
- **Kotlin** 1.9.22
- **Jetpack Compose** 1.6.1
- **Material3** 1.2.0

### Arquitetura
- **MVVM** (Model-View-ViewModel)
- **StateFlow** para gerenciamento de estado
- **Coroutines** para operações assíncronas
- **Navigation Compose** para navegação

### Imagens
- **Coil** 2.5.0 (carregamento de imagens)
- Suporte a GIF
- Cache automático

### Media
- **Media3** 1.5.1 (mantido do original)
- **ExoPlayer** para reprodução
- Background playback

### Database
- **Room** 2.6.1
- **Room KTX** para coroutines

## 🎯 Próximos Passos

### Prioridade Alta
1. ✅ Configuração e estrutura base
2. ⏳ Converter ViewModels para Kotlin
3. ⏳ Implementar tela de biblioteca
4. ⏳ Implementar player completo
5. ⏳ Integrar com MediaService

### Prioridade Média
- Tela de busca
- Detalhes de álbum/artista
- Menus e dialogs
- Animações

### Prioridade Baixa
- Temas customizados
- Tablets e landscape
- Animações avançadas

## 🤝 Contribuindo

### Convenções de Código

1. **Kotlin Style Guide**
   - Use camelCase para variáveis e funções
   - Use PascalCase para classes e Composables
   - Indentação: 4 espaços

2. **Composables**
   - Sempre comece com letra maiúscula
   - Use `@Composable` annotation
   - Crie previews com `@Preview`

3. **ViewModels**
   - Sempre hereditário de `ViewModel()`
   - Use `StateFlow` para observação
   - Use `viewModelScope` para coroutines

4. **Estrutura de Arquivos**
   - 1 Composable principal por arquivo
   - Helpers no mesmo arquivo (se pequenos)
   - Preview no final do arquivo

### Exemplo de Composable
```kotlin
@Composable
fun MyComponent(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Implementation
}

@Preview
@Composable
fun MyComponentPreview() {
    shibamusicTheme {
        MyComponent(title = "Preview")
    }
}
```

## 🐛 Problemas Conhecidos

### Fontes
- Se as fontes Circular não carregarem, edite `Type.kt`
- Use fontes do sistema como fallback

### Imagens
- Coil requer permissão INTERNET
- Adicione placeholders para melhor UX

### Performance
- Listas grandes podem precisar paginação
- Use `key()` em LazyColumn/Grid

## 📖 Recursos de Aprendizado

### Jetpack Compose
- [Official Compose Tutorial](https://developer.android.com/jetpack/compose/tutorial)
- [Compose Samples](https://github.com/android/compose-samples)
- [Material3 Guidelines](https://m3.material.io/)

### Kotlin
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

### Arquitetura
- [Android Architecture Guide](https://developer.android.com/topic/architecture)
- [StateFlow vs LiveData](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)

## 🔥 Performance Tips

1. **Minimize Recomposition**
   ```kotlin
   val state by remember { mutableStateOf(value) }
   ```

2. **Use Stable Keys**
   ```kotlin
   LazyColumn {
       items(list, key = { it.id }) { item ->
           // Item
       }
   }
   ```

3. **Defer State Reads**
   ```kotlin
   modifier = Modifier.drawBehind {
       val color = colorState.value // Read inside callback
   }
   ```

## 📄 Licença

O Tempo mantém sua licença original. Esta migração para Compose é parte do projeto Tempo.

---

**Status**: 🚧 Em desenvolvimento ativo
**Versão**: 0.1.0-compose-alpha
**Última atualização**: Outubro 2025
