// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.chtrembl.petstoreassistant;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.chtrembl.petstoreassistant.model.AzurePetStoreSessionInfo;
import com.chtrembl.petstoreassistant.model.DPResponse;
import com.chtrembl.petstoreassistant.service.IAzureOpenAI;
import com.chtrembl.petstoreassistant.service.IAzurePetStore;
import com.chtrembl.petstoreassistant.service.AzureOpenAI.Classification;
import com.chtrembl.petstoreassistant.utility.PetStoreAssistantUtilities;
import com.codepoetics.protonpack.collectors.CompletableFutures;
import com.microsoft.bot.builder.ActivityHandler;
import com.microsoft.bot.builder.MessageFactory;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.schema.ChannelAccount;

/**
 * This class implements the functionality of the Bot.
 *
 * <p>
 * This is where application specific logic for interacting with the users would
 * be added. For this
 * sample, the {@link #onMessageActivity(TurnContext)} echos the text back to
 * the user. The {@link
 * #onMembersAdded(List, TurnContext)} will send a greeting to new conversation
 * participants.
 * </p>
 */
@Component
@Primary
public class PetStoreAssistantBot extends ActivityHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PetStoreAssistantBot.class);

    @Autowired
    private IAzureOpenAI azureOpenAI;
   
    @Autowired
    private IAzurePetStore azurePetStore;

    @Override
    protected CompletableFuture<Void> onMessageActivity(TurnContext turnContext) {
        String text = turnContext.getActivity().getText().toLowerCase();

        this.logTurnContext(turnContext);
        
        // strip out session id and csrf token
        AzurePetStoreSessionInfo azurePetStoreSessionInfo = PetStoreAssistantUtilities.getAzurePetStoreSessionInfo(text);
        if(azurePetStoreSessionInfo != null)
        {
            text = azurePetStoreSessionInfo.getNewText();
        }

        DPResponse dpResponse = this.azureOpenAI.classification(text);
 
        switch (dpResponse.getClassification()) {
            case UPDATE_SHOPPING_CART:
                if(azurePetStoreSessionInfo != null)
                {
                    dpResponse = this.azureOpenAI.completion("find the product that is associated with the following text: \'" + text + "\'", Classification.SEARCH_FOR_PRODUCTS);
                    if(dpResponse.getResponseProductIDs() != null && dpResponse.getResponseProductIDs().size() == 1)
                    {
                        dpResponse = this.azurePetStore.updateCart(azurePetStoreSessionInfo, dpResponse.getResponseProductIDs().get(0));
                    }
                }
                else {
                    dpResponse.setDpResponseText("Once I get your session information, I will be able to update your shopping cart.");
                }
                break;
            case VIEW_SHOPPING_CART:
                dpResponse.setDpResponseText("Once I get your session information, I will be able to display your shopping cart.");
                break;
            case PLACE_ORDER:
                dpResponse.setDpResponseText("Once I get your session information, I will be able to place your order.");
                break;
            case SEARCH_FOR_PRODUCTS:
                dpResponse = this.azureOpenAI.completion(text, dpResponse.getClassification());
                break;
            case SOMETHING_ELSE:
                dpResponse = this.azureOpenAI.completion(text, dpResponse.getClassification());
                break; 
        }

        return turnContext.sendActivity(
                MessageFactory.text(dpResponse.getDpResponseText())).thenApply(sendResult -> null);
    }

    @Override
    protected CompletableFuture<Void> onMembersAdded(
            List<ChannelAccount> membersAdded,
            TurnContext turnContext) {

                logTurnContext(turnContext);


        return membersAdded.stream()
                .filter(
                        member -> !StringUtils
                                .equals(member.getId(), turnContext.getActivity().getRecipient().getId()))
                .map(channel -> turnContext
                        .sendActivity(
                                MessageFactory.text("Hello and welcome to the Azure Pet Store, you can ask me questions about our products, your shopping cart and your order, you can also ask me for information about pet animals. How can I help you?")))
                .collect(CompletableFutures.toFutureList()).thenApply(resourceResponses -> null);
    }

    private void logTurnContext(TurnContext turnContext)
    {

         try
        {
            LOGGER.info("trying to getRecipient id");
            LOGGER.info(turnContext.getActivity().getRecipient().getId());
        }
        catch(Exception e)
        {
            LOGGER.info("could not getRecipient " + e.getMessage());
        }

        try
        {
            LOGGER.info("trying to get entities");
            Object object = turnContext.getActivity().getEntities();
            LOGGER.info("found a entities object" + object.getClass() + " " + object.toString());
        }
        catch(Exception e)
        {
            LOGGER.info("could not get entities " + e.getMessage());
        }

        try
        {
            LOGGER.info("trying to channelData");
            Object object = turnContext.getActivity().getChannelData();
            LOGGER.info("found a channel data object" + object.getClass()+ " " + object.toString());
        
            LOGGER.info( turnContext.getActivity().getChannelData().toString() );
        }
        catch(Exception e)
        {
            LOGGER.info("could not get channelData " + e.getMessage());
        }

        try
        {
            LOGGER.info("trying to getProperties");
            turnContext.getActivity().getProperties().entrySet().iterator().forEachRemaining(entry ->  LOGGER.info(entry.getKey() + " " + entry.getValue()));
       
        }
        catch(Exception e)
        {
            LOGGER.info("could not get getProperties " + e.getMessage());
        }
        
        try
        {
            LOGGER.info("trying to getSumamry");
            LOGGER.info(turnContext.getActivity().getSummary());
        }
        catch(Exception e)
        {
            LOGGER.info("could not get getSumamry " + e.getMessage());
        }

        try
        {
            LOGGER.info("trying to getRecipient");
            turnContext.getActivity().getRecipient().getProperties().entrySet().iterator().forEachRemaining(entry -> LOGGER.info(entry.getKey() + " " + entry.getValue()));
        }
        catch(Exception e)
        {
            LOGGER.info("could not get getRecipient " + e.getMessage());
        }

        try
        {
            LOGGER.info("trying to getConversation");
              turnContext.getActivity().getConversation().getProperties().entrySet().iterator().forEachRemaining(entry -> LOGGER.info(entry.getKey() + " " + entry.getValue()));
        }
        catch(Exception e)
        {
            LOGGER.info("could not get getConversation " + e.getMessage());
        }

        try
        {
            LOGGER.info("trying to getFrom");
            turnContext.getActivity().getFrom().getProperties().entrySet().iterator().forEachRemaining(entry -> LOGGER.info(entry.getKey() + " " + entry.getValue()));
        }
        catch(Exception e)
        {
            LOGGER.info("could not get getFrom " + e.getMessage());
        }
    }
}
