package veil;

import java.util.*;

/**
 * Host-only receipt transfer from an actual powered, faction-owned factory to
 * the physical runway/rail stock ledger. No arbitrary item/quantity is
 * minted: the source output is debited while both ledgers are locked.
 *
 * The caller MUST be a server-verified settlement owner, not a client-sent ID.
 * The source factory snapshot and destination snapshot must be persisted in
 * the SAME authoritative checkpoint to avoid crash-between-saves duplication.
 */
public final class TransportFactoryDelivery {
    private TransportFactoryDelivery(){}
    public enum Result { DELIVERED, DUPLICATE, DENIED, NOT_READY, NO_ROOM }
    public static Result deliverToConstruction(int verifiedCaller,
            IndustrialWorkstation factory,TransportInfrastructure construction,
            TransportProduction.Product product,int count,String receipt){
        if(factory==null||construction==null||product==null||
           receipt==null||!receipt.matches("[a-zA-Z0-9_-]{6,80}")||
           count<1||count>512||verifiedCaller!=construction.ownerId())
            return Result.DENIED;
        // One lock order for all transport deliveries: factory then ledger.
        synchronized(factory){
            synchronized(construction){
                if(construction.delivered(receipt))return Result.DUPLICATE;
                CharacterRoster.Faction faction=construction.faction();
                var process=TransportProduction.recipe(product,faction);
                if(factory.faction()!=TransportProduction.industryFaction(faction)||
                   !factory.facilityId().equals(process.facility()))
                    return Result.DENIED;
                // All target construction items are manufactured at the
                // faction's transport works, not at a generic or naval factory.
                if(!Set.of(TransportProduction.Product.RUNWAY_PANEL,
                        TransportProduction.Product.RAIL_SECTION,
                        TransportProduction.Product.TRANSPORT_REPAIR,
                        TransportProduction.Product.RUNWAY_LIGHT,
                        TransportProduction.Product.RAIL_SIGNAL).contains(product))
                    return Result.DENIED;
                String item=process.output();
                if(factory.count(item)<count)return Result.NOT_READY;
                if(!construction.canAcceptDelivery(item,count,receipt))
                    return Result.NO_ROOM;
                if(!factory.withdraw(item,count))return Result.NOT_READY;
                // Preflight above means credit cannot fail. If an unexpected
                // in-process exception occurs, return exact goods to the
                // still-locked source machine rather than silently lose them.
                try{
                    construction.acceptDelivery(item,count,receipt);
                    return Result.DELIVERED;
                }catch(RuntimeException exception){
                    if(!factory.deposit(item,count))
                        throw new IllegalStateException("Critical delivery rollback failure",
                            exception);
                    throw exception;
                }
            }
        }
    }
}
