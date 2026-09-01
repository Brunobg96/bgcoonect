BG CONNECT GESTOR ANDROID 1.1 — PUSH FCM

Este projeto recebe novos pedidos mesmo com o painel fechado, usando Firebase Cloud Messaging.

Antes de compilar:
1) No Firebase, crie um app Android com package com.bgconnect.gestor.
2) Abra app/src/main/res/values/firebase_config.xml e substitua os 4 valores.
3) No servidor, instale o ZIP BG CONNECT V11.3.24 e coloque a conta de serviço em config/firebase-service-account.json.
4) Execute upgrade.php no servidor.
5) Compile o APK no Android Studio e instale.
6) Permita notificações e faça login no app.

O token FCM é vinculado automaticamente à loja do usuário autenticado.
Ao sair pelo botão Sair do painel, o dispositivo é desativado daquela conta.
