package com.pubnub.components.example.getting_started.ui.view


import android.util.Log
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.paging.PagingData
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import com.pubnub.api.models.consumer.pubsub.PNSignalResult
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult
import com.pubnub.api.models.consumer.pubsub.objects.PNObjectEventResult
import com.pubnub.components.chat.ui.component.input.MessageInput
import com.pubnub.components.chat.ui.component.input.renderer.AnimatedTypingIndicatorRenderer
import com.pubnub.components.chat.ui.component.menu.Copy
import com.pubnub.components.chat.ui.component.menu.React
import com.pubnub.components.chat.ui.component.message.MessageList
import com.pubnub.components.chat.ui.component.message.MessageUi
import com.pubnub.components.chat.ui.component.presence.Presence
import com.pubnub.components.chat.ui.component.provider.LocalChannel
import com.pubnub.components.chat.ui.component.provider.LocalPubNub
import com.pubnub.components.chat.viewmodel.message.MessageViewModel
import com.pubnub.components.chat.viewmodel.message.ReactionViewModel
import com.pubnub.framework.data.ChannelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

object Chat {

    @Composable
    internal fun Content(
        messages: Flow<PagingData<MessageUi>>,
        presence: Presence? = null,
        onMessageSelected: (MessageUi.Data) -> Unit,
        onReactionSelected: ((React) -> Unit)? = null,
    ) {
        val localFocusManager = LocalFocusManager.current
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        localFocusManager.clearFocus()
                    })
                }
        ) {
            MessageList(
                messages = messages,
                presence = presence,
                onMessageSelected = onMessageSelected,
                onReactionSelected = onReactionSelected,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f, true),
            )

            MessageInput(
                typingIndicator = true,
                typingIndicatorRenderer = AnimatedTypingIndicatorRenderer,
            )
        }
    }

    @Composable
    fun View(
        channelId: ChannelId,
    ) {
        // region Content data
        val messageViewModel: MessageViewModel = MessageViewModel.defaultWithMediator(channelId)
        val messages = remember { messageViewModel.getAll() }

        val reactionViewModel: ReactionViewModel = ReactionViewModel.default()
        // endregion

        var menuVisible by remember { mutableStateOf(false) }
        var selectedMessage by remember { mutableStateOf<MessageUi.Data?>(null) }

        CompositionLocalProvider(LocalChannel provides channelId) {
            val crs = rememberCoroutineScope()
            val pubnub = LocalPubNub.current
            LaunchedEffect(true){
                crs.launch {
;                   setupListen(pubnub)
                }
            }
            Menu(
                visible = menuVisible,
                message = selectedMessage,
                onDismiss = { menuVisible = false },
                onAction = { action ->
                    when (action) {
                        is Copy -> {
                            action.message.text?.let { content ->
                                messageViewModel.copy(AnnotatedString(content))
                            }
                        }
                        is React -> reactionViewModel.reactionSelected(action)
                        else -> {}
                    }
                }
            )


            Content(
                messages = messages,
                onMessageSelected = {
                    selectedMessage = it
                    menuVisible = true
                },
                onReactionSelected = reactionViewModel::reactionSelected,
            )
        }
    }

    private fun setupListen(pubnub: PubNub) {
        pubnub.addListener(object : SubscribeCallback() {
            override fun status(pubnub: PubNub, pnStatus: PNStatus) {
                Log.d("Chat", "Status category: ${pnStatus.category}")
                // PNConnectedCategory, PNReconnectedCategory, PNDisconnectedCategory

                Log.d("Chat", "Status operation: ${pnStatus.operation}")
                // PNSubscribeOperation, PNHeartbeatOperation

                Log.d("Chat", "Status error: ${pnStatus.error}")
                // true or false
            }

            override fun presence(pubnub: PubNub, pnPresenceEventResult: PNPresenceEventResult) {
                Log.d("Chat", "Presence event: ${pnPresenceEventResult.event}")
                Log.d("Chat", "Presence channel: ${pnPresenceEventResult.channel}")
                Log.d("Chat", "Presence uuid: ${pnPresenceEventResult.uuid}")
                Log.d("Chat", "Presence timetoken: ${pnPresenceEventResult.timetoken}")
                Log.d("Chat", "Presence occupancy: ${pnPresenceEventResult.occupancy}")
            }

            override fun message(pubnub: PubNub, pnMessageResult: PNMessageResult) {
                Log.d("Chat", "Message payload: ${pnMessageResult.message}")
                Log.d("Chat", "Message channel: ${pnMessageResult.channel}")
                Log.d("Chat", "Message publisher: ${pnMessageResult.publisher}")
                Log.d("Chat", "Message timetoken: ${pnMessageResult.timetoken}")
            }

            override fun signal(pubnub: PubNub, pnSignalResult: PNSignalResult) {
                Log.d("Chat", "Signal payload: ${pnSignalResult.message}")
                Log.d("Chat", "Signal channel: ${pnSignalResult.channel}")
                Log.d("Chat", "Signal publisher: ${pnSignalResult.publisher}")
                Log.d("Chat", "Signal timetoken: ${pnSignalResult.timetoken}")
            }

            override fun messageAction(pubnub: PubNub, pnMessageActionResult: PNMessageActionResult) {
                with(pnMessageActionResult.messageAction) {
                    Log.d("Chat", "Message action type: $type")
                    Log.d("Chat", "Message action value: $value")
                    Log.d("Chat", "Message action uuid: $uuid")
                    Log.d("Chat", "Message action actionTimetoken: $actionTimetoken")
                    Log.d("Chat", "Message action messageTimetoken: $messageTimetoken")
                }

                Log.d("Chat", "Message action subscription: ${pnMessageActionResult.subscription}")
                Log.d("Chat", "Message action channel: ${pnMessageActionResult.channel}")
                Log.d("Chat", "Message action timetoken: ${pnMessageActionResult.timetoken}")
            }

            override fun objects(pubnub: PubNub, objectEvent: PNObjectEventResult) {
                Log.d("Chat", "Object event channel: ${objectEvent.channel}")
                Log.d("Chat", "Object event publisher: ${objectEvent.publisher}")
                Log.d("Chat", "Object event subscription: ${objectEvent.subscription}")
                Log.d("Chat", "Object event timetoken: ${objectEvent.timetoken}")
                Log.d("Chat", "Object event userMetadata: ${objectEvent.userMetadata}")

                with(objectEvent.extractedMessage) {
                    Log.d("Chat", "Object event event: $event")
                    Log.d("Chat", "Object event source: $source")
                    Log.d("Chat", "Object event type: $type")
                    Log.d("Chat", "Object event version: $version")
                }
            }
        })
    }
}

@Composable
@Preview
private fun ChatPreview() {
    Chat.View("channel.lobby")
}
