# rfw.notifier
Projeto com classes para facilitar a notificação de desenvolvedores ou usuários através de sistemas comuns (como telegram, slack, email, whatsapp, sms, etc.)

- `TelegramNotifier`: Envio de mensagens e arquivos via bot do Telegram.
- `SlackNotifier`: Envio de mensagens para canais, conversas e usuários do Slack via Web API.

## Configuração do Slack

1. Crie um aplicativo em <https://api.slack.com/apps> e habilite um **Bot User**. Gere o **Bot User OAuth Token** (formato `xoxb-...`) com escopos mínimos `chat:write`, `im:write`, `channels:read`, `groups:read` e `users:read` para enviar mensagens e resolver IDs.
2. No canal desejado, clique em **Canal > About > More > Copy channel ID** para obter `SLACK_CHANNEL_ID` e defina-o também como `SLACK_DEFAULT_CHANNEL` se quiser um destino padrão.
3. Para mensagens diretas, abra o perfil do usuário, escolha **More** e copie o **Member ID** para preencher `SLACK_USER_ID`.
4. Caso precise testar conversas já existentes (por exemplo, um grupo privado), copie o ID visível no menu de contexto ou via opção **Copy link**, extraindo o trecho após `archives/` para `SLACK_CONVERSATION_ID`.
5. No arquivo `srcTest/resources/slacktest.properties`, substitua os valores de exemplo por seus IDs reais:
   - `SLACK_BOT_TOKEN`
   - `SLACK_DEFAULT_CHANNEL`
   - `SLACK_CHANNEL_ID`
   - `SLACK_CONVERSATION_ID`
   - `SLACK_USER_ID`
6. Execute os testes somente após preencher o arquivo para permitir o envio das notificações reais.
