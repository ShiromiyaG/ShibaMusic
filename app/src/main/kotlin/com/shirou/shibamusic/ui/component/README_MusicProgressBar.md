# Barra de Progresso Musical - ShibaMusic

## Overview

Este módulo implementa uma barra de progresso musical moderna e flexível para o ShibaMusic, inspirada em padrões de design contemporâneos de players de música.

## Componentes Principais

### `MusicProgressBar`
Componente principal que oferece diferentes estilos de visualização do progresso musical:

- **LINEAR**: Barra tradicional com thumb interativo
- **LINEAR_MINIMAL**: Barra minimalista sem thumb
- **CIRCULAR**: Progresso circular ao redor de um botão
- **CURVED**: Design curvo moderno

### `ModernMiniPlayer`
Mini player com design moderno que integra:
- Progresso circular ao redor da thumbnail
- Suporte a gestos de swipe
- Botões de favorito e subscrição
- Animações suaves

## Características Técnicas

### Animações
- **Spring Animation**: Para transições suaves
- **Linear Easing**: Para progresso contínuo durante a reprodução
- **Tween Animation**: Para mudanças de estado

### Gestos e Interação
- **Swipe Horizontal**: Navegação entre faixas
- **Click/Tap**: Controle de reprodução
- **Drag**: Ajuste de posição na música

### Responsividade
- Adaptação automática a diferentes tamanhos de tela
- Suporte a RTL (Right-to-Left)
- Window Insets para navegação gestual

## Uso Básico

```kotlin
// Progress bar simples
MusicProgressBar(
    progress = 0.4f,
    onProgressChange = { newProgress -> 
        // Atualizar posição da música
    },
    style = MusicProgressBarStyle.LINEAR
)

// Progress bar circular
CircularMusicProgressBar(
    progress = 0.6f,
    onProgressChange = { /* handle change */ },
    isPlaying = true,
    onPlayPauseClick = { /* toggle play/pause */ }
)
```

## Configuração Avançada

```kotlin
MusicProgressBar(
    progress = currentProgress,
    onProgressChange = { newProgress ->
        // Lógica de atualização
    },
    onProgressChangeFinished = {
        // Ação ao finalizar ajuste
    },
    style = MusicProgressBarStyle.CIRCULAR,
    isPlaying = isPlayingState,
    trackColor = CustomColors.trackColor,
    progressColor = CustomColors.primaryColor,
    thumbColor = CustomColors.accentColor,
    animateProgress = true
)
```

## Design System

### Cores
- **Track Color**: `MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)`
- **Progress Color**: `MaterialTheme.colorScheme.primary`
- **Thumb Color**: `MaterialTheme.colorScheme.primary`
- **Error Color**: `MaterialTheme.colorScheme.error`

### Dimensões
- **Track Height**: 4.dp (padrão)
- **Thumb Size**: 20.dp (padrão)
- **Circular Size**: 48.dp (padrão)
- **Stroke Width**: 3.dp (progress circular)

### Formas
- **Circular**: `CircleShape`
- **Rounded**: `RoundedCornerShape(32.dp)` para containers
- **Track**: `RoundedCornerShape(trackHeight / 2)` para barras lineares

## Melhorias Implementadas

1. **Performance**
   - Animações otimizadas com `animateFloatAsState`
   - Lazy loading de componentes
   - Memória de estados para evitar recomposições

2. **Acessibilidade**
   - Content descriptions apropriadas
   - Suporte a navegação por teclado
   - Contrast ratios adequados

3. **UX/UI**
   - Feedback tátil com animações
   - Indicadores visuais claros
   - Consistência com Material Design 3

## Próximos Passos

- [ ] Implementar controle por gestos avançados
- [ ] Adicionar suporte a equalização visual
- [ ] Integrar com sistema de themes personalizados
- [ ] Otimizar para tablets e telas grandes
- [ ] Adicionar testes unitários

## Licença

Este código faz parte do projeto ShibaMusic e segue a mesma licença do projeto principal.
