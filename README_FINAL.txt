BG CONNECT GESTOR - Android + Firebase Push
Versao: 1.2.0
Pacote Android: com.bgconnect.gestor
Servidor: https://bgconnect.kinghost.net/admin.php

CONFIGURACAO JA INCLUIDA
- google-services.json do projeto Firebase BG CONNECT
- Firebase Cloud Messaging
- permissao POST_NOTIFICATIONS para Android 13+
- notificacao de novo pedido em alta prioridade
- ao tocar no Push, abre o pedido no painel
- token FCM e enviado ao BG CONNECT depois que o gestor faz login
- upload de arquivos/imagens no WebView
- cookies/sessao persistentes

SERVIDOR
Use BG CONNECT DELIVERY V11.3.25 ou superior.
Mantenha o JSON privado da conta de servico na pasta config do servidor.
Execute upgrade.php uma vez se ainda nao executou a atualizacao que cria push_devices.

GERAR APK NO ANDROID STUDIO
1. Extraia este ZIP.
2. Abra a pasta BGConnectGestor no Android Studio.
3. Aguarde o Gradle Sync terminar.
4. Build > Build Bundle(s) / APK(s) > Build APK(s).
5. O APK de teste sera criado normalmente em app/build/outputs/apk/debug/app-debug.apk.

PARA DISTRIBUICAO
Use Build > Generate Signed App Bundle / APK e crie/guarde uma chave de assinatura (keystore).
Nao perca essa chave: futuras atualizacoes do mesmo app precisam ser assinadas pela mesma chave.

TESTE DO PUSH
1. Instale o APK no Android.
2. Permita notificacoes.
3. Abra o app e faca login como proprietario/gestor da loja.
4. Deixe o app em segundo plano ou bloqueie a tela.
5. Crie um novo pedido nessa loja por outro aparelho/navegador.
6. O Android deve receber o Push "Novo pedido".

OBSERVACAO
O google-services.json pertence ao app Android e pode ficar dentro do projeto do aplicativo.
O JSON da conta de servico do Firebase e PRIVADO e deve ficar somente no servidor, nunca dentro do APK.
