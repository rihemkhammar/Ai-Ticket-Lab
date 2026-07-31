import { TbCheck, TbX, TbPointFilled, TbMinus } from 'react-icons/tb';
import { PIPELINE_STAGES, isInvestigationApplicable, ROUTING_DECISION_CONFIG } from './Triageconstants';

/**
 * Stepper horizontal : Classify -> Order -> Dispatch -> Investigation ->
 * Review -> Rules -> HITL.
 *
 * Le backend (TriageTreatedItem) n'expose pas d'étape courante par ticket
 * — seulement `criticality` et `outcome` (SUCCESS | FAILED). On reconstruit
 * donc l'état de chaque étape à partir de ces deux champs, avec les mêmes
 * règles que TriagePipelineService côté backend :
 *
 * - CLASSIFY / ORDER / DISPATCH : toujours atteintes (le ticket est dans
 *   `treated`, donc il a forcément été classifié puis dispatché).
 * - INVESTIGATION : exécutée seulement si criticality est CRITICAL ou HIGH
 *   (même seuil que TriagePipelineService#isCriticalEnoughForInvestigation) ;
 *   sinon affichée comme "ignorée", pas comme "échouée".
 * - RULES : marquée "terminée" dès que `routingDecision` (résultat réel de
 *   RulesNode, remonté par le back) est présent — pas déduite de `outcome`,
 *   qui décrit l'issue de HITL, pas celle de Rules.
 * - REVIEW / HITL : marquées "terminées" si outcome === SUCCESS.
 *   Si outcome === FAILED, le backend ne dit pas à quelle étape précise ça
 *   s'est arrêté (Rule 2.7 : une seule erreur agrégée, pas de détail par
 *   étape) — on les affiche donc comme "interrompues", sans en désigner une
 *   comme fautive à tort. Le vrai message d'erreur reste affiché sous le
 *   stepper (item.errorMessage) par TriageTicketCard.
 */
export default function TriagePipelineSteps({ criticality, outcome, routingDecision }) {
  const investigationRan = isInvestigationApplicable(criticality);
  const succeeded = outcome === 'SUCCESS';
  const failed = outcome === 'FAILED';

  const statusFor = (key) => {
    if (key === 'CLASSIFY' || key === 'ORDER' || key === 'DISPATCH') return 'done';
    if (key === 'INVESTIGATION') return investigationRan ? (succeeded || failed ? 'done' : 'pending') : 'skipped';
    if (key === 'RULES') {
      // Le back ne fait remonter routingDecision que si RulesNode a
      // réellement tourné (voir HitlCheckpointNode) — indépendamment de
      // outcome, qui décrit l'issue de HITL, pas celle de Rules.
      if (routingDecision) return 'done';
      if (failed) return 'interrupted';
      return 'pending';
    }
    // REVIEW / HITL
    if (succeeded) return 'done';
    if (failed) return 'interrupted';
    return 'pending';
  };

  return (
    <div style={{ display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '2px' }}>
      {PIPELINE_STAGES.map((stage, i) => {
        const status = statusFor(stage.key);

        const color = status === 'interrupted' ? '#f87171'
          : status === 'skipped' ? '#64748b'
          : status === 'done' ? '#4ade80'
          : '#facc15';

        const Icon = status === 'interrupted' ? TbX
          : status === 'skipped' ? TbMinus
          : status === 'done' ? TbCheck
          : TbPointFilled;

        const rulesTitle = stage.key === 'RULES' && routingDecision
          ? (ROUTING_DECISION_CONFIG[routingDecision]?.label ?? routingDecision)
          : undefined;

        return (
          <div key={stage.key} style={{ display: 'flex', alignItems: 'center' }}>
            <div
              title={rulesTitle
                ?? (status === 'skipped' ? 'Skipped — non-critical ticket' : status === 'interrupted' ? 'Pipeline interrupted before or at this step' : undefined)}
              style={{
                display: 'flex', alignItems: 'center', gap: '5px',
                padding: '4px 9px', borderRadius: '999px',
                background: `${color}1a`, border: `1px solid ${color}40`,
                opacity: status === 'skipped' ? 0.55 : 1,
              }}
            >
              <Icon size={11} color={color} />
              <span style={{ fontSize: '11px', fontWeight: 600, color, whiteSpace: 'nowrap' }}>
                {stage.label}
              </span>
            </div>
            {i < PIPELINE_STAGES.length - 1 && (
              <div style={{ width: '10px', height: '1px', background: 'rgba(148,163,184,0.25)' }} />
            )}
          </div>
        );
      })}
    </div>
  );
}