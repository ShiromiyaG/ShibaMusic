# Funcionalidade de Música Offline - ShibaMusic

## Visão Geral

Esta implementação adiciona uma funcionalidade completa de música offline ao ShibaMusic, inspirada no projeto [Tempo](https://github.com/eddyizm/tempo). A funcionalidade permite aos usuários baixar músicas do servidor Subsonic/Navidrome para reprodução offline com gapless playback.

## Funcionalidades Implementadas

### ✅ Core Features
- **Download de Músicas**: Baixar músicas individuais ou álbuns completos para armazenamento local
- **Reprodução Offline**: Player dedicado com suporte a gapless playback usando ExoPlayer
- **Gerenciamento de Cache**: Sistema inteligente de limpeza e otimização de armazenamento
- **Downloads em Background**: Worker que continua downloads mesmo com app fechado
- **Interface Intuitiva**: UI moderna usando Jetpack Compose com Material3

### 🎵 Player Offline
- Reprodução gapless entre faixas
- Controles completos (play, pause, skip, seek)
- Suporte a playlists offline
- Modo shuffle e repeat
- Informações de qualidade de áudio

### 📱 Interface do Usuário
- **Tela Offline Dedicada**: Lista todas as músicas disponíveis offline
- **Indicadores de Status**: Mostra progresso de download em tempo real
- **Informações de Armazenamento**: Exibe uso de espaço e estatísticas
- **Controles de Gestão**: Botões para limpar cache, verificar integridade

### 🔧 Gerenciamento de Cache
- **Limpeza Automática**: Remove músicas antigas quando necessário
- **Verificação de Integridade**: Detecta e remove arquivos corrompidos
- **Estatísticas Detalhadas**: Informações sobre uso de espaço e número de faixas
- **Política LRU**: Remove conteúdo menos recente primeiro

## Arquitetura

### Camadas Implementadas

```
┌─────────────────────────────────────────┐
│                UI Layer                 │
│  OfflineScreen.kt + OfflineViewModel.kt │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│              Domain Layer               │
│           OfflineRepository.kt          │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│               Data Layer                │
│  OfflineTrackDao.kt + ShibaMusicDB.kt  │
└─────────────────────────────────────────┘
                    │
┌─────────────────────────────────────────┐
│             Service Layer               │
│  OfflineDownloadService.kt + Worker.kt  │
└─────────────────────────────────────────┘
```

### Componentes Principais

1. **Modelos de Dados**
   - `OfflineTrack`: Entidade Room para metadados de músicas
   - `DownloadProgress`: Controle de progresso de downloads
   - `AudioQuality`: Enum para qualidades de áudio (Low/Medium/High)

2. **Camada de Dados**
   - `OfflineTrackDao`: Interface DAO com operações CRUD
   - `ShibaMusicDatabase`: Database Room com migrações
   - `DatabaseModule`: Injeção de dependência com Hilt

3. **Serviços**
   - `OfflineDownloadService`: Service foreground para downloads
   - `OfflineDownloadWorker`: WorkManager para downloads em background
   - `OfflineMusicPlayer`: Player customizado com ExoPlayer

4. **Repositório**
   - `OfflineRepository`: Coordena operações entre DAO e serviços
   - Gerenciamento de estado e caching

5. **UI Components**
   - `OfflineScreen`: Interface principal usando Compose
   - `OfflineViewModel`: Gerenciamento de estado da UI
   - `OfflineNavigation`: Integração com Navigation Compose

6. **Utilitários**
   - `OfflineCacheManager`: Gestão avançada de cache
   - Limpeza automática e verificação de integridade

## Configuração

### Dependências Adicionadas
- `androidx.room:room-*`: Banco de dados local
- `androidx.media3:media3-*`: Player com gapless playback
- `androidx.work:work-runtime-ktx`: Downloads em background
- `androidx.hilt:hilt-work`: Integração Hilt + WorkManager

### Permissões
- `WRITE_EXTERNAL_STORAGE` (API < 29)
- `READ_MEDIA_AUDIO` (API 33+)
- `FOREGROUND_SERVICE_DATA_SYNC`
- `ACCESS_NETWORK_STATE`

### Configurações
- Database versão 2 com migração automática
- Notificações para progresso de download
- Worker constraints (WiFi, bateria)

## Como Usar

### Para Desenvolvedores

1. **Integrar na Navegação Principal**:
```kotlin
// No seu NavHost principal
offlineScreen() // Adiciona a rota offline
```

2. **Iniciar Download**:
```kotlin
viewModel.downloadTrack(
    trackId = "123",
    title = "Nome da Música",
    artist = "Artista",
    album = "Álbum",
    duration = 240000L,
    originalUrl = "https://server.com/stream/123",
    quality = AudioQuality.MEDIUM
)
```

3. **Reproduzir Offline**:
```kotlin
viewModel.playOfflineTrack("123")
// ou reproduzir tudo
viewModel.playAllOffline()
```

### Para Usuários

1. **Baixar Música**: Toque no ícone de download em qualquer música
2. **Ver Progresso**: Navegue até a tela "Offline" para ver downloads
3. **Reproduzir**: Toque em qualquer música na lista offline
4. **Gerenciar**: Use botões para limpar cache ou verificar integridade

## Benefícios da Implementação

### 🚀 Performance
- Reprodução instantânea sem buffering
- Gapless playback para experiência premium
- Cache inteligente reduz uso de dados

### 💾 Eficiência de Armazenamento
- Compressão otimizada baseada na qualidade escolhida
- Limpeza automática de arquivos antigos
- Verificação de integridade remove corruptos

### 🔋 Economia de Bateria
- Downloads em background otimizados
- Reprodução local reduz uso de rede
- WorkManager respeitam constraints de bateria

### 📱 Experiência do Usuário
- Interface intuitiva com Material3
- Indicadores visuais claros de status
- Controles familiares de media player

## Próximos Passos

### Melhorias Futuras
- [ ] Sincronização automática de favoritos
- [ ] Download de playlists completas
- [ ] Compressão de áudio em tempo real
- [ ] Integração com Android Auto
- [ ] Estatísticas de uso offline

### Otimizações
- [ ] Pool de connections para downloads paralelos
- [ ] Retry automático com backoff exponencial
- [ ] Pré-cache baseado em histórico de escuta
- [ ] Compressão de metadados

Esta implementação fornece uma base sólida para funcionalidades offline avançadas, seguindo as melhores práticas do Android e mantendo compatibilidade com a arquitetura existente do ShibaMusic.