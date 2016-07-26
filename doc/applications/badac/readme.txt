This directory contains for BADAC:

1. Action model file
2. Loadable Lumen code required by its Lumen agent (detectorSelection.lumen)
3. Some testing code (detectorSelectionTesting.lumen)

To run Lumen-side only test:

1. Make sure Lumen is on your path.

2.

% cd <this directory>
% lumen detectorSelectionTesting.lumen
-lumen> do 'DETECT_ENTITY/0'(1,2,$e)

You'll see:

Scoring \<ActionExprLumen$ActionClosureInstance@1280156865:action(\<Expr detectOpenField(+$image, +$classifier, -$entities)>\, properties:\<Expr {detector:"true", falseAlarmRate:"0.05", minSamples:"1000", requiresMSIData:"true", viewpoint:"overhead"}>\, execute:\<Expr {test \<Expr $entities = [`\<Expr openField1>\, `\<Expr openField2>\]>\;}>\)>\ requiresMSIData="true" 
Scoring \<ActionExprLumen$ActionClosureInstance@390397675:action(\<Expr detectRunway(+$image, +$classifier, -$entities)>\, properties:\<Expr {detector:"true", falseAlarmRate:"0.01", minSamples:"1000", requiresMSIData:"false", viewpoint:"overhead"}>\, execute:\<Expr {test \<Expr $entities = [`\<Expr runway31>\]>\;}>\)>\ requiresMSIData="false" 
Scoring \<ActionExprLumen$ActionClosureInstance@243095098:action(\<Expr detectHAS(+$image, +$classifier, -$entities)>\, properties:\<Expr {detector:"true", falseAlarmRate:"0.02", minSamples:"1000", requiresMSIData:"false", viewpoint:"overhead"}>\, execute:\<Expr {test \<Expr $entities = [`\<Expr HAS1>\]>\;}>\)>\ requiresMSIData="false" 
Score=1.0  best detector: <ActionExprLumen$ActionClosureInstance@1280156865:action(\<Expr detectOpenField(+$image, +$classifier, -$entities)>\, properties:\<Expr {detector:"true", falseAlarmRate:"0.05", minSamples:"1000", requiresMSIData:"true", viewpoint:"overhead"}>\, execute:\<Expr {test \<Expr $entities = [`\<Expr openField1>\, `\<Expr openField2>\]>\;}>\)>

Task succeeded: $e=[openField1, openField2] 
-lumen>
