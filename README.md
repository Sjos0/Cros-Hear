# Dot Crosshair

Aplicativo Android leve para exibir uma pequena mira em forma de ponto sobre jogos e outros aplicativos. O ponto pode ser ativado/desativado, redimensionado e personalizado com cores predefinidas.

## Baixar

**[Baixar DotCrosshair-v1.0.apk na página de Releases](https://github.com/Sjos0/Cros-Hear/releases/download/v1.0.0/DotCrosshair-v1.0.apk)**

O APK também está versionado no repositório em [`apk/DotCrosshair-v1.0.apk`](apk/DotCrosshair-v1.0.apk). A cópia [`apk/DotCrosshair.apk`](apk/DotCrosshair.apk) tem atualmente o mesmo SHA-256; o arquivo com versão no nome é o recomendado para downloads e releases.

### Integridade do APK v1.0

- **SHA-256:** `0b60047918bf24dd314548ab07bbd12b9a591e5bf6a5f75a74b87909971b48dc`
- **Tamanho:** aproximadamente 9,7 MiB (10.114.270 bytes)
- **Formato:** APK Android; a estrutura ZIP foi verificada sem erros.

## Funcionalidades

- Mira centralizada na tela em formato de ponto.
- Controle para ativar e desativar a sobreposição.
- Tamanho ajustável de 2 a 24 dp.
- Oito cores predefinidas, incluindo verde neon, ciano, vermelho, amarelo, magenta, branco, laranja e preto.
- Preferências de tamanho, cor e estado salvas localmente.
- Serviço em primeiro plano com notificação para manter a mira ativa.
- A sobreposição não recebe foco nem bloqueia os toques destinados ao aplicativo abaixo dela.

## Compatibilidade

- **Android mínimo:** Android 8.0 (API 26), conforme `minSdk` do projeto.
- **Arquiteturas incluídas no APK:** ARM64 (`arm64-v8a`), ARM de 32 bits (`armeabi-v7a`), x86 e x86-64.
- **Moto G8 Plus:** a versão Android e a arquitetura do aparelho se enquadram nos requisitos declarados. A instalação e o comportamento não foram validados fisicamente nesse modelo; restrições de bateria ou personalizações do fabricante podem afetar serviços em segundo plano.

## Instalação no Android

1. Abra a página [Releases](https://github.com/Sjos0/Cros-Hear/releases) e baixe `DotCrosshair-v1.0.apk`.
2. Abra o APK no dispositivo. Se solicitado, permita a instalação de aplicativos da fonte usada para baixar o arquivo.
3. Abra **Dot Crosshair** e toque em **Conceder Permissão**.
4. Nas configurações do Android, permita que o aplicativo seja exibido sobre outros aplicativos.
5. Ative a mira e ajuste o tamanho e a cor. No Android 13 ou superior, permita notificações se quiser ver a notificação do serviço.

A permissão de sobreposição é necessária para mostrar o ponto sobre jogos e outros aplicativos. Para interromper a mira, use o controle no aplicativo ou a ação de desativação da notificação.

## Compilar a partir do código

O projeto usa Kotlin, Jetpack Compose e Gradle. Para compilar localmente, instale o JDK exigido pela versão do Android Gradle Plugin, o Android SDK com a plataforma usada pelo projeto (API 36.1) e permita que o Gradle Wrapper baixe a distribuição configurada.

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

O APK de depuração é gerado em `app/build/outputs/apk/debug/`. Para criar uma build de release assinada, configure seu próprio keystore e as variáveis `KEYSTORE_PATH`, `STORE_PASSWORD` e `KEY_PASSWORD`; as credenciais de assinatura não são incluídas neste repositório.

**Limitação conhecida:** esses comandos não foram executados nesta publicação. Uma tentativa anterior de compilação do código-fonte falhou por conflito de recursos dos ícones do launcher; a compilação reproduzível ainda precisa ser corrigida e validada separadamente. A release v1.0.0 usa o APK existente em `apk/`, não um binário recompilado durante esta tarefa.

## Permissões e privacidade

O manifesto declara permissões para sobreposição de tela, serviço em primeiro plano (incluindo o tipo especial usado pelo serviço) e notificações. O manifesto atual não declara a permissão `INTERNET`. As preferências do aplicativo são armazenadas localmente.

## Testes e estado do projeto

Há testes automatizados para preferências, limites de tamanho e criação/atualização do componente visual. Este README não representa uma certificação de testes em dispositivos físicos; execute os testes e valide a instalação no aparelho de destino antes de distribuir uma nova build.

## Licença

Este repositório ainda não contém um arquivo `LICENSE`. A licença do código precisa ser definida pelo mantenedor antes de anunciar termos de uso, redistribuição ou contribuição de terceiros.
