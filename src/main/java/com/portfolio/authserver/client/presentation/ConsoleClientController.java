package com.portfolio.authserver.client.presentation;

import com.portfolio.authserver.client.application.ClientService;
import com.portfolio.authserver.client.domain.Client;
import com.portfolio.authserver.client.presentation.mapper.ClientMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("/console/realms/{realmName}/clients")
@RequiredArgsConstructor
public class ConsoleClientController {

    private final ClientService clientService;
    private final ClientMapper clientMapper;

    @GetMapping
    public String listClients(@PathVariable String realmName, Model model) {
        model.addAttribute("realmName", realmName);
        model.addAttribute("clients", clientService.listClients(realmName));
        return "console/clients";
    }

    @PostMapping
    public String createClient(@PathVariable String realmName,
                               @RequestParam String clientId, @RequestParam String clientSecret,
                               @RequestParam String redirectUris,
                               @RequestParam(defaultValue = "openid,profile,offline_access") String scopes,
                               @RequestParam(required = false) Boolean requireProofKey,
                               @RequestParam(required = false) Boolean requireAuthorizationConsent,
                               RedirectAttributes redirectAttributes) {
        try {
            Client client = clientService.createClient(realmName, clientId, clientSecret,
                    ClientService.splitCommaSeparated(redirectUris), ClientService.splitCommaSeparated(scopes),
                    requireProofKey != null, requireAuthorizationConsent != null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Client '" + client.getClientId() + "' created");
        } catch (NoSuchElementException | IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/clients";
    }

    @GetMapping("/{clientId}/edit")
    public String editForm(@PathVariable String realmName, @PathVariable String clientId, Model model) {
        Client client = clientService.getClient(realmName, clientId);
        RegisteredClient registeredClient = clientMapper.toRegisteredClient(client);


        model.addAttribute("realmName", realmName);
        model.addAttribute("client", client);
        model.addAttribute("redirectUrisJoined", String.join(",", client.getRedirectUris()));
        model.addAttribute("scopesJoined", String.join(",", client.getScopes()));
        model.addAttribute("requireProofKey", registeredClient.getClientSettings().isRequireProofKey());
        model.addAttribute("requireAuthorizationConsent",
                registeredClient.getClientSettings().isRequireAuthorizationConsent());
        return "console/client-edit";
    }

    @PostMapping("/{clientId}/update")
    public String update(@PathVariable String realmName, @PathVariable String clientId,
                         @RequestParam String redirectUris,
                         @RequestParam String scopes,
                         @RequestParam(required = false) Boolean requireProofKey,
                         @RequestParam(required = false) Boolean requireAuthorizationConsent,
                         @RequestParam(required = false) String newClientSecret,
                         RedirectAttributes redirectAttributes) {
        try {
            clientService.updateClient(realmName, clientId,
                    ClientService.splitCommaSeparated(redirectUris), ClientService.splitCommaSeparated(scopes),
                    requireProofKey != null,
                    requireAuthorizationConsent != null, newClientSecret);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Client '"+clientId+"' updated");
        } catch (NoSuchElementException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/" + realmName + "/clients";
    }

    @PostMapping("/{clientId}/delete")
    public String deleteClient(@PathVariable String realmName, @PathVariable String clientId,
                                       RedirectAttributes redirectAttributes) {
        try{
            clientService.deleteClient(realmName, clientId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Deleted client '"+clientId+"'");
        }catch (NoSuchElementException ex){
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/console/realms/"+realmName+"/clients";
    }
}