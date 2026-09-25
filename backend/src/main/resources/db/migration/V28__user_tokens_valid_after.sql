-- "Sair de todos os dispositivos": tokens emitidos antes desta data deixam de valer, sem lista de revogação.
ALTER TABLE public."user" ADD COLUMN tokens_valid_after TIMESTAMP WITH TIME ZONE;
